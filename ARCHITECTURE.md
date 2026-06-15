# Auth Service — Documento de Arquitectura

> Senior Architect Design Document  
> Proyecto: Trust | Servicio: auth-service  
> Stack: Java 17 · Spring WebFlux · Programación Reactiva · Gradle · Arquitectura Hexagonal  
> Infraestructura: AWS Cognito · DynamoDB · Resend · ECS Fargate · ALB

---

## 0. Estándares de arquitectura hexagonal

Todos los microservicios de Trust siguen estos estándares sin excepción.

### Estructura de capas

```
src/main/java/com/trust/{servicio}/
│
├── domain/                          ← núcleo de negocio, cero dependencias externas
│   ├── model/
│   │   ├── {Entidad}.java           ← modelo de dominio (@Getter @Builder con Lombok)
│   │   ├── command/
│   │   │   └── {Accion}Command.java ← modelo de ENTRADA del caso de uso (record)
│   │   └── result/
│   │       └── {Accion}Result.java  ← modelo de SALIDA del caso de uso (record)
│   ├── exception/
│   │   └── {Nombre}Exception.java   ← excepciones de negocio (extienden RuntimeException)
│   ├── port/
│   │   ├── in/
│   │   │   └── {Accion}UseCase.java ← interfaz: recibe Command, retorna Mono<Result>
│   │   └── out/
│   │       └── {Recurso}Port.java   ← interfaz: lo que el dominio necesita del exterior
│   └── service/
│       └── {Accion}Service.java     ← implementa UseCase, orquesta ports out (@Service)
│
└── infrastructure/                  ← detalles técnicos, frameworks, AWS
    ├── adapter/
    │   ├── in/web/
    │   │   ├── controller/
    │   │   │   └── {Contexto}Controller.java   ← @RestController, delega al UseCase
    │   │   ├── dto/
    │   │   │   ├── request/{Accion}Request.java ← record con validaciones (@NotBlank, @Email)
    │   │   │   └── response/{Accion}Response.java ← record de salida HTTP
    │   │   ├── mapper/
    │   │   │   └── {Contexto}WebMapper.java    ← MapStruct: Request→Command, Result→Response
    │   │   └── GlobalExceptionHandler.java     ← @RestControllerAdvice con ProblemDetail
    │   └── out/
    │       ├── dynamodb/
    │       │   ├── entity/
    │       │   │   └── {Nombre}Entity.java     ← @DynamoDbBean para el Enhanced Client
    │       │   ├── mapper/
    │       │   │   └── {Nombre}DynamoMapper.java ← MapStruct: dominio→entidad
    │       │   └── {Nombre}DynamoAdapter.java  ← implementa Port, usa mapper + DynamoDB
    │       ├── cognito/
    │       │   └── CognitoAdapter.java         ← implementa CognitoPort
    │       └── email/
    │           └── ResendEmailAdapter.java     ← implementa EmailPort
    └── config/
        ├── AwsConfig.java           ← beans: DynamoDbEnhancedClient, CognitoClient
        ├── ResendConfig.java        ← bean: Resend
        └── DynamoDbProperties.java ← @ConfigurationProperties(prefix="dynamodb.tables")
```

### Reglas que no se rompen

| Regla | Por qué |
|-------|---------|
| El dominio no importa nada de `infrastructure` | Independencia total del framework |
| Los `Command` son `record` inmutables | Entrada limpia y sin setters |
| Los `Result` son `record` inmutables | Salida predecible |
| Los adaptadores no construyen entidades — usan mappers | Single responsibility |
| Los nombres de tablas vienen de `DynamoDbProperties` | Sin strings hardcodeados |
| Los controllers no tienen lógica — solo delegan | Thin controllers |
| Las excepciones de negocio se lanzan en el `Service` | El dominio decide qué es un error |
| El `GlobalExceptionHandler` traduce excepciones a HTTP | Separación entre negocio e HTTP |

### Flujo completo de una petición

```
HTTP Request
    ↓
Controller  →  mapper.toCommand(request)
    ↓
UseCase.execute(command)
    ↓
Service  →  valida, orquesta ports out
    ↓
Port out (DynamoDB / Cognito / Resend)
    ↓
Service  →  construye Result
    ↓
Controller  →  mapper.toResponse(result)
    ↓
HTTP Response
```

### Manejo de errores

```
Service lanza excepción de dominio (ej: CompanyCodeAlreadyExistsException)
    ↓
GlobalExceptionHandler la captura
    ↓
Retorna ProblemDetail con el código HTTP correcto (409, 400, 403, etc.)
```

---

## 1. Responsabilidad del servicio

El `auth-service` es el único servicio del sistema que interactúa con **AWS Cognito** y emite **JWT enriquecidos** con el contexto del tenant (CDA). Ningún otro microservicio llama a Cognito directamente.

Responsabilidades:
- Onboarding de CDAs (creación del primer usuario admin)
- Login y emisión de JWT con `cda_id`, `role`, `scopes[]`
- Primer login: forzar cambio de contraseña + completar perfil
- Creación de usuarios internos por el admin del CDA
- Refresh de token
- Logout
- Recuperación de contraseña

---

## 2. Estructura hexagonal

```
auth-service/
└── src/main/java/com/trust/auth/
    │
    ├── domain/                          ← Núcleo de negocio (sin dependencias externas)
    │   ├── model/                       ← Entidades y value objects del dominio
    │   │   ├── User.java
    │   │   ├── Cda.java
    │   │   ├── UserCda.java
    │   │   ├── Role.java
    │   │   └── Scope.java               ← Enum: GERENCIAL, ADMINISTRADOR, CALIDAD, RRHH
    │   │
    │   ├── port/
    │   │   ├── in/                      ← Interfaces de casos de uso (lo que entra)
    │   │   │   ├── LoginUseCase.java
    │   │   │   ├── OnboardUserUseCase.java
    │   │   │   ├── CreateCdaUseCase.java
    │   │   │   ├── CreateWorkerUseCase.java
    │   │   │   └── RefreshTokenUseCase.java
    │   │   │
    │   │   └── out/                     ← Interfaces de salida (lo que el dominio necesita)
    │   │       ├── CognitoPort.java
    │   │       ├── UserRepositoryPort.java
    │   │       ├── CdaRepositoryPort.java
    │   │       ├── UserCdaRepositoryPort.java
    │   │       └── EmailPort.java
    │   │
    │   └── service/                     ← Implementaciones de los casos de uso
    │       ├── LoginService.java
    │       ├── OnboardUserService.java
    │       ├── CreateCdaService.java
    │       ├── CreateWorkerService.java
    │       └── RefreshTokenService.java
    │
    └── infrastructure/                  ← Detalles técnicos (frameworks, AWS, DB)
        ├── adapter/
        │   ├── in/                      ← Adaptadores de ENTRADA
        │   │   └── web/
        │   │       ├── controller/
        │   │       │   ├── AuthController.java
        │   │       │   ├── OnboardingController.java
        │   │       │   └── UserManagementController.java
        │   │       ├── dto/
        │   │       │   ├── request/
        │   │       │   │   ├── LoginRequest.java
        │   │       │   │   ├── OnboardRequest.java
        │   │       │   │   ├── CreateCdaRequest.java
        │   │       │   │   ├── CreateWorkerRequest.java
        │   │       │   │   └── RefreshTokenRequest.java
        │   │       │   └── response/
        │   │       │       ├── LoginResponse.java
        │   │       │       ├── TokenResponse.java
        │   │       │       └── UserResponse.java
        │   │       └── mapper/
        │   │           ├── AuthWebMapper.java
        │   │           └── UserWebMapper.java
        │   │
        │   └── out/                     ← Adaptadores de SALIDA
        │       ├── cognito/
        │       │   └── CognitoAdapter.java
        │       ├── dynamodb/
        │       │   ├── UserDynamoAdapter.java
        │       │   ├── CdaDynamoAdapter.java
        │       │   └── UserCdaDynamoAdapter.java
        │       ├── ses/
        │       │   └── SesEmailAdapter.java
        │       └── mapper/
        │           ├── UserDynamoMapper.java
        │           └── CdaDynamoMapper.java
        │
        └── config/
            ├── AwsConfig.java
            ├── SecurityConfig.java
            ├── DynamoDbConfig.java
            └── JwtConfig.java
```

---

## 3. Flujos de negocio

### Flujo 1 — Trust activa un CDA (Onboarding del cliente)

```
Trust Admin (backoffice)
    │
    │  POST /internal/cdas
    │  { name, company_code, admin_email, admin_name }
    ▼
CreateCdaUseCase
    │
    ├─ 1. Validar que company_code no exista → CdaRepositoryPort
    ├─ 2. Crear CDA en DynamoDB → CdaRepositoryPort.save()
    ├─ 3. Crear usuario en Cognito con temp password
    │      → CognitoPort.adminCreateUser(email, temp_password)
    │      Cognito marca al usuario como FORCE_CHANGE_PASSWORD
    ├─ 4. Guardar user en DynamoDB → UserRepositoryPort.save()
    ├─ 5. Crear relación user↔CDA con rol "Administrador" y todos los scopes
    │      → UserCdaRepositoryPort.save()
    └─ 6. Enviar email de invitación → EmailPort.sendCdaInvitation()
           Template: "Tu CDA fue activado en Trust.
                      Ingresa con: {email} / {temp_password}
                      Código de empresa: {company_code}"
```

**Decisión de diseño — ¿quién envía el email?**

Se usará **AWS SES** (Simple Email Service), NO el email nativo de Cognito porque:
- Cognito tiene límite de 50 emails/día en sandbox
- No permite personalizar bien el template
- SES permite dominio propio (ej: `noreply@trust.com.co`)
- Control total del contenido y branding

Cognito se configura en modo `DEVELOPER` para que NO envíe su email por defecto — lo manejamos nosotros vía SES.

---

### Flujo 2 — Primer login (completar perfil y cambiar contraseña)

```
Usuario recibe email con credenciales temporales
    │
    │  POST /auth/login
    │  { company_code, email, password }
    ▼
LoginService
    │
    ├─ 1. Buscar CDA por company_code → CdaRepositoryPort
    ├─ 2. Autenticar en Cognito → CognitoPort.initiateAuth(email, password)
    │
    │  Cognito retorna: ChallengeName = NEW_PASSWORD_REQUIRED
    │  (el usuario tiene contraseña temporal)
    │
    └─ 3. Retornar al frontend: { status: "REQUIRES_ONBOARDING", session_token }
           El frontend redirige a /onboarding

    │
    │  POST /auth/onboard
    │  { session_token, new_password, name, phone, document_number }
    ▼
OnboardUserService
    │
    ├─ 1. Responder challenge en Cognito con nueva contraseña
    │      → CognitoPort.respondToNewPasswordChallenge(session, new_password)
    ├─ 2. Actualizar perfil del usuario en DynamoDB
    │      → UserRepositoryPort.completeProfile(user_id, name, phone, doc)
    ├─ 3. Marcar usuario como onboarded (para no pedir datos de nuevo)
    │      → UserRepositoryPort.markOnboarded(user_id)
    └─ 4. Emitir JWT enriquecido con contexto del CDA
           { user_id, cda_id, role, scopes[], email }
```

**Importante:** El campo `onboarded: boolean` en DynamoDB evita volver a pedirle datos al usuario en logins futuros. Solo se hace UNA vez.

---

### Flujo 3 — Login normal (usuario ya onboarded)

```
POST /auth/login
{ company_code, email, password }
    │
    ▼
LoginService
    │
    ├─ 1. Buscar CDA por company_code → cda_id
    ├─ 2. Autenticar con Cognito → AccessToken, IdToken, RefreshToken
    ├─ 3. Obtener user_id del IdToken (claim: sub → cognito_sub → user_id)
    ├─ 4. Buscar relación user↔CDA en DynamoDB
    │      → UserCdaRepositoryPort.find(user_id, cda_id)
    │      Retorna: role, scopes[], active
    ├─ 5. Verificar que el usuario esté activo en ese CDA
    └─ 6. Emitir JWT propio de Trust
           {
             user_id, cda_id, role, scopes[],
             email, name,
             exp: ahora + 1h
           }
           + RefreshToken (guardado o devuelto según estrategia)
```

**Nota sobre los dos tokens:**
- **Cognito AccessToken**: solo se usa internamente para verificar credenciales. No sale al frontend.
- **Trust JWT**: es el token que usa el frontend y los demás microservicios. Contiene el contexto del CDA.

---

### Flujo 4 — Admin del CDA crea un trabajador

```
POST /users/workers
Headers: Authorization: Bearer {trust_jwt}
{ email, name, role_name, scopes: ["calidad", "rrhh"] }
    │
    ▼
CreateWorkerService
    │
    ├─ 1. Verificar que el caller tiene scope 'administrador' en su JWT
    ├─ 2. Verificar que el email no exista ya en trust_users
    │      └─ Si existe (user en otro CDA): no crear en Cognito, solo vincular
    │      └─ Si no existe: crear en Cognito + crear en trust_users
    ├─ 3. Crear relación user↔CDA con rol y scopes asignados
    │      → UserCdaRepositoryPort.save({ user_id, cda_id, role, scopes })
    └─ 4. Enviar email de invitación al trabajador
           Template: "Fuiste invitado a {CDA_NAME} en Trust.
                      Código de empresa: {company_code}
                      Credenciales temporales: {email} / {temp_password}"
```

**Caso especial — Juanito ya existe en otro CDA:**
- NO se crea una nueva cuenta en Cognito (ya tiene su cuenta)
- Solo se crea el registro en `trust_user_cda` con el nuevo CDA
- Se le envía email avisando que fue agregado al nuevo CDA
- En su próximo login con ese `company_code` ya tendrá acceso

---

### Flujo 5 — Refresh Token

```
POST /auth/refresh
{ refresh_token }
    │
    ▼
RefreshTokenService
    │
    ├─ 1. Validar refresh_token (firmado con secreto interno)
    ├─ 2. Extraer user_id y cda_id del refresh_token
    ├─ 3. Verificar que el user sigue activo en ese CDA → DynamoDB
    └─ 4. Emitir nuevo JWT de acceso (1h) + nuevo refresh token (7d)
```

---

### Flujo 6 — Recuperación de contraseña

```
POST /auth/forgot-password
{ email }
    │
    └─ CognitoPort.forgotPassword(email)
       Cognito envía código al email del usuario
       (aquí SÍ usamos Cognito para el envío del código,
        es un email transaccional simple sin branding)

POST /auth/confirm-forgot-password
{ email, confirmation_code, new_password }
    │
    └─ CognitoPort.confirmForgotPassword(email, code, new_password)
```

---

## 4. Estrategia de emails (AWS SES)

| Evento | Template | Enviado por |
|--------|----------|------------|
| CDA activado → primer admin | `cda-invitation` | auth-service vía SES |
| Admin crea trabajador nuevo | `worker-invitation` | auth-service vía SES |
| Trabajador agregado a otro CDA | `cda-added` | auth-service vía SES |
| Recuperación de contraseña | código numérico | Cognito directamente |

**Configuración SES:**
- Dominio verificado: `trust.com.co`
- From: `noreply@trust.com.co`
- Templates almacenados en SES (no hardcodeados)
- Cognito configurado en modo `DEVELOPER` → no envía emails propios

---

## 5. Modelo DynamoDB

### Tabla: `trust_users`
```
PK: user_id (UUID)
Atributos: cognito_sub, email, name, phone, document_number,
           onboarded (boolean), active, created_at
```

### Tabla: `trust_cdas`
```
PK: cda_id (UUID)
Atributos: company_code, name, active, created_at
GSI: company_code-index → PK: company_code (búsqueda en login)
```

### Tabla: `trust_user_cda`
```
PK: cda_id
SK: user_id
Atributos: role (String), scopes (List<String>), active, created_at
GSI: user-index → PK: user_id (para ver todos los CDAs de un user)
```

---

## 6. Endpoints del servicio

### Públicos (sin JWT)
| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/auth/login` | Login inicial |
| POST | `/auth/onboard` | Completar perfil (primer login) |
| POST | `/auth/refresh` | Refresh token |
| POST | `/auth/forgot-password` | Solicitar reset |
| POST | `/auth/confirm-forgot-password` | Confirmar reset |

### Protegidos — scope: `administrador`
| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/users/workers` | Crear trabajador |
| GET | `/users/workers` | Listar trabajadores del CDA |
| PUT | `/users/workers/{id}` | Actualizar rol/scopes |
| DELETE | `/users/workers/{id}` | Desactivar trabajador |

### Internos — solo entre microservicios (header interno)
| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/internal/cdas` | Trust activa un CDA |
| GET | `/internal/validate-token` | Validar JWT para otros servicios |

---

## 7. Variables de entorno requeridas

```env
# AWS
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=

# Cognito
AWS_COGNITO_USER_POOL_ID=us-east-1_XXXXXXX
AWS_COGNITO_CLIENT_ID=
AWS_COGNITO_CLIENT_SECRET=

# DynamoDB
AWS_DYNAMODB_ENDPOINT=          # vacío en prod, apunta al QA en desarrollo

# Resend (email)
RESEND_API_KEY=re_xxxxxxxxxxxx
RESEND_FROM_EMAIL=noreply@trust.com.co

# JWT
JWT_SECRET=                     # mínimo 256 bits
FRONTEND_URL=https://app.trust.com.co
```

---

## 8. Decisiones de arquitectura

| Decisión | Opción elegida | Razón |
|----------|---------------|-------|
| Framework | Spring WebFlux | Programación reactiva, no bloquea hilos, mejor rendimiento con AWS SDK v2 async |
| Email | Resend (no AWS SES) | 3.000 emails/mes gratis, API simple, sin configuración de sandbox ni dominio verificado en AWS |
| Token | JWT propio de Trust (no Cognito token) | Incluye `cda_id` y `scopes[]` que Cognito no tiene nativamente |
| Onboarding | Challenge `NEW_PASSWORD_REQUIRED` de Cognito | Flujo nativo de AWS, no hay que reinventar el manejo de temp passwords |
| Multi-CDA user | Un Cognito account, múltiples registros en `trust_user_cda` | El código de empresa en el login determina el contexto |
| Email en creación de worker | Siempre enviar invitación | El trabajador puede no existir en Cognito o puede ser un user existente en otro CDA |
| Entorno local | Perfiles AWS apuntando a QA | No se usa DynamoDB local — el ambiente QA en AWS sirve como entorno de desarrollo |

---

## 9. Pendientes y consideraciones futuras

- [ ] Suspensión de un CDA completo por Trust (desactiva todos sus users)
- [ ] Límite de usuarios por CDA según plan contratado
- [ ] Auditoría de accesos (quién, cuándo, desde qué IP)
- [ ] MFA opcional para roles con scope `gerencial`
- [ ] Expiración automática de invitaciones (temp password expira en 7 días en Cognito por defecto)
- [ ] Rate limiting en `/auth/login` para evitar fuerza bruta
- [ ] Notificación al admin cuando un trabajador completa su onboarding
