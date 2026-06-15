# Auth Service — Casos de Uso

> Documento de diseño de casos de uso  
> Proyecto: Trust | Servicio: auth-service  
> Actor principal: Trabajador, Admin CDA, Trust Admin (backoffice)

---

## Mapa de casos de uso

```
┌─────────────────────────────────────────────────────────────────┐
│                        AUTH SERVICE                             │
│                                                                 │
│  AUTENTICACIÓN              ONBOARDING           USUARIOS       │
│  ─────────────              ──────────           ────────       │
│  UC-01 Login                UC-04 Activar CDA   UC-07 Crear     │
│  UC-02 Refresh Token        UC-05 Completar      trabajador     │
│  UC-03 Logout                    Perfil          UC-08 Listar   │
│                                                   trabajadores  │
│  CONTRASEÑA                 INTERNO              UC-09 Actualizar│
│  ───────────                ────────             UC-10 Desactivar│
│  UC-11 Olvidé contraseña    UC-12 Validar Token                 │
│  UC-13 Confirmar reset                                          │
└─────────────────────────────────────────────────────────────────┘
```

---

## UC-01 — Login

**Actor:** Cualquier usuario registrado (trabajador, admin CDA)  
**Trigger:** El usuario ingresa sus credenciales en la pantalla de login

### Precondiciones
- El CDA debe existir y estar activo
- El usuario debe existir en Cognito y en `trust_user_cda` para ese CDA
- El usuario debe haber completado su onboarding (campo `onboarded: true`)

### Flujo principal
```
1. Usuario envía: company_code, email, password
2. Sistema busca el CDA por company_code → obtiene cda_id
3. Sistema autentica en Cognito con email + password
4. Cognito retorna AccessToken, IdToken, RefreshToken
5. Sistema extrae cognito_sub del IdToken
6. Sistema busca user_id por cognito_sub en trust_users
7. Sistema busca relación user↔CDA en trust_user_cda
8. Sistema verifica que el usuario esté activo en ese CDA
9. Sistema emite JWT de Trust con: user_id, cda_id, role, scopes[], email
10. Sistema retorna: { access_token, refresh_token, expires_in }
```

### Flujos alternativos
| Condición | Resultado |
|-----------|-----------|
| CDA no existe o inactivo | 404 — "Código de empresa no válido" |
| Credenciales incorrectas | 401 — "Correo o contraseña incorrectos" |
| Usuario no pertenece al CDA | 403 — "No tienes acceso a esta empresa" |
| Usuario inactivo en el CDA | 403 — "Tu acceso fue desactivado" |
| Cognito retorna `NEW_PASSWORD_REQUIRED` | 200 — `{ status: REQUIRES_ONBOARDING, session_token }` |
| Demasiados intentos fallidos | 429 — "Cuenta bloqueada temporalmente" |

### Postcondiciones
- JWT de Trust emitido y válido por 1 hora
- Refresh token válido por 7 días

---

## UC-02 — Refresh Token

**Actor:** Frontend (automático cuando el access_token expira)  
**Trigger:** El access_token está próximo a expirar o ya expiró

### Precondiciones
- El refresh_token debe ser válido y no expirado
- El usuario debe seguir activo en el CDA del token

### Flujo principal
```
1. Frontend envía: refresh_token
2. Sistema valida la firma y expiración del refresh_token
3. Sistema extrae user_id y cda_id del refresh_token
4. Sistema verifica que el user sigue activo en ese CDA → DynamoDB
5. Sistema emite nuevo access_token (1h) y nuevo refresh_token (7d)
6. Retorna: { access_token, refresh_token, expires_in }
```

### Flujos alternativos
| Condición | Resultado |
|-----------|-----------|
| refresh_token expirado | 401 — redirigir al login |
| refresh_token inválido o manipulado | 401 — "Token no válido" |
| Usuario desactivado en el CDA | 403 — "Tu acceso fue desactivado" |

---

## UC-03 — Logout

**Actor:** Usuario autenticado  
**Trigger:** El usuario hace clic en "Cerrar sesión"

### Flujo principal
```
1. Frontend envía: Authorization: Bearer {access_token}
2. Sistema agrega el token a una blacklist (DynamoDB TTL)
   → Tabla: trust_token_blacklist | PK: jti (token id) | TTL: exp del token
3. Sistema retorna: 200 OK
4. Frontend elimina tokens del almacenamiento local
```

### Nota de diseño
No se invalida en Cognito porque el token de Cognito nunca sale al frontend. El JWT de Trust se invalida mediante blacklist con TTL automático de DynamoDB.

---

## UC-04 — Activar CDA (Trust Admin)

**Actor:** Administrador interno de Trust (backoffice)  
**Trigger:** Un CDA firma contrato y paga — Trust activa su cuenta

### Precondiciones
- El `company_code` no debe existir previamente
- El `admin_email` no debe estar bloqueado

### Flujo principal
```
1. Trust Admin envía: name, company_code, admin_email, admin_name
2. Sistema valida que company_code sea único
3. Sistema crea el CDA en DynamoDB (trust_cdas)
4. Sistema crea usuario admin en Cognito con contraseña temporal
   → Cognito: AdminCreateUser con SUPPRESS (no envía email de Cognito)
5. Sistema guarda el usuario en DynamoDB (trust_users)
6. Sistema crea relación user↔CDA con todos los scopes
   → trust_user_cda: scopes = [gerencial, administrador, calidad, rrhh]
7. Sistema envía email de invitación vía SES
   → Template: cda-invitation
   → Contenido: nombre del CDA, company_code, email, contraseña temporal
8. Retorna: { cda_id, company_code, admin_email, status: ACTIVATED }
```

### Flujos alternativos
| Condición | Resultado |
|-----------|-----------|
| company_code ya existe | 409 — "Código de empresa ya registrado" |
| Error al crear en Cognito | Rollback: eliminar CDA de DynamoDB |
| Error al enviar email SES | Continúa, registra en logs para reintento manual |

### Postcondiciones
- CDA activo en el sistema
- Primer admin invitado y pendiente de onboarding

### Endpoint

**`POST /internal/cdas`**

**Autorización:** Header `X-Internal-Key: {clave compartida}` — solo backoffice interno de Trust.

**Request body:**
```json
{
  "name": "CDA Medellín Norte",
  "company_code": "MDN-001",
  "admin_email": "admin@cdamedellin.com",
  "admin_name": "Carlos Pérez"
}
```

**Response `201`:**
```json
{
  "cda_id": "uuid",
  "company_code": "MDN-001",
  "admin_email": "admin@cdamedellin.com",
  "status": "ACTIVATED"
}
```

**Errores:**
| Código | Condición |
|--------|-----------|
| 409 | `company_code` ya existe |
| 500 + rollback | Falla Cognito → se elimina el CDA creado en DynamoDB |
| 201 + log | Falla Resend → CDA queda activo, email se reintenta manualmente |

---

## UC-05 — Completar Perfil (Primer Login)

**Actor:** Cualquier usuario que recibió invitación  
**Trigger:** Login retorna `{ status: REQUIRES_ONBOARDING, session_token }`

### Precondiciones
- El `session_token` de Cognito debe ser válido (expira en 3 minutos)
- El usuario debe tener `onboarded: false` en DynamoDB

### Flujo principal
```
1. Usuario envía: session_token, new_password, name, phone, document_number
2. Sistema valida que new_password cumpla política (mayúscula, número, especial, 8+ chars)
3. Sistema responde challenge en Cognito:
   → RespondToAuthChallenge: NEW_PASSWORD_REQUIRED
   → new_password enviada a Cognito
4. Cognito confirma cambio y retorna tokens definitivos
5. Sistema actualiza perfil en DynamoDB (trust_users):
   → name, phone, document_number, onboarded: true
6. Sistema emite JWT de Trust con contexto del CDA
7. Retorna: { access_token, refresh_token, expires_in }
```

### Flujos alternativos
| Condición | Resultado |
|-----------|-----------|
| session_token expirado | 401 — "Sesión expirada, inicia sesión de nuevo" |
| Contraseña no cumple política | 400 — Detalle de reglas incumplidas |
| Usuario ya hizo onboarding | 409 — "Perfil ya completado" |

### Postcondiciones
- Usuario con `onboarded: true` en DynamoDB
- Perfil completo (nombre, teléfono, documento)
- JWT emitido, usuario dentro del sistema

---

## UC-06 — Olvidé mi Contraseña

**Actor:** Usuario registrado  
**Trigger:** Clic en "¿Olvidaste tu contraseña?" en el login

### Flujo principal
```
1. Usuario envía: email
2. Sistema verifica que el email exista en trust_users
3. Sistema llama a Cognito: ForgotPassword(email)
   → Cognito envía código de 6 dígitos al email (aquí SÍ usa Cognito email)
4. Retorna: 200 — "Si el correo existe, recibirás un código"
   (siempre 200 para no revelar si el email existe)
```

### Nota de seguridad
Siempre retornar 200 aunque el email no exista. Evita enumerar usuarios válidos.

---

## UC-07 — Confirmar Reset de Contraseña

**Actor:** Usuario que solicitó reset  
**Trigger:** El usuario recibe el código por email

### Flujo principal
```
1. Usuario envía: email, confirmation_code, new_password
2. Sistema llama a Cognito: ConfirmForgotPassword(email, code, new_password)
3. Cognito valida el código (expira en 1 hora)
4. Contraseña actualizada en Cognito
5. Retorna: 200 — "Contraseña actualizada exitosamente"
```

### Flujos alternativos
| Condición | Resultado |
|-----------|-----------|
| Código inválido o expirado | 400 — "Código incorrecto o expirado" |
| Contraseña no cumple política | 400 — Detalle de reglas |

---

## UC-08 — Crear Trabajador

**Actor:** Usuario con scope `administrador`  
**Trigger:** Admin del CDA agrega un nuevo trabajador desde el panel

### Precondiciones
- El caller debe tener scope `administrador` en su JWT
- El email del trabajador puede o no existir en el sistema

### Flujo principal
```
1. Admin envía: email, name, role_name, scopes[]
   (Headers: Authorization: Bearer {jwt_admin})
2. Sistema verifica scope 'administrador' en el JWT
3. Sistema extrae cda_id del JWT
4. Sistema busca si el email ya existe en trust_users

   → CASO A: Email NO existe (usuario nuevo en el sistema)
     a. Crear usuario en Cognito (AdminCreateUser + SUPPRESS)
     b. Guardar en trust_users
     c. Crear en trust_user_cda con role y scopes
     d. Enviar email: template worker-invitation (con temp password)

   → CASO B: Email SÍ existe (usuario de otro CDA)
     a. NO crear en Cognito (ya tiene cuenta)
     b. Verificar que NO esté ya en este CDA
     c. Crear solo en trust_user_cda con role y scopes
     d. Enviar email: template cda-added (sin temp password)

5. Retorna: { user_id, email, role, scopes, status: INVITED }
```

### Flujos alternativos
| Condición | Resultado |
|-----------|-----------|
| Caller sin scope `administrador` | 403 — "No tienes permiso para esta acción" |
| Email ya pertenece a este CDA | 409 — "Este usuario ya es parte de tu empresa" |
| Scope inválido enviado | 400 — "Scope no reconocido: {scope}" |

---

## UC-09 — Listar Trabajadores del CDA

**Actor:** Usuario con scope `administrador`

### Flujo principal
```
1. Admin hace GET /users/workers
2. Sistema extrae cda_id del JWT
3. Sistema consulta trust_user_cda por cda_id
4. Sistema enriquece con datos de trust_users (nombre, email)
5. Retorna lista: [{ user_id, name, email, role, scopes, active, created_at }]
```

---

## UC-10 — Actualizar Rol y Scopes de Trabajador

**Actor:** Usuario con scope `administrador`

### Flujo principal
```
1. Admin envía: PUT /users/workers/{user_id}
   Body: { role_name, scopes[] }
2. Sistema verifica scope 'administrador' en JWT
3. Sistema verifica que el user_id pertenezca al cda_id del JWT
4. Sistema actualiza trust_user_cda: role, scopes
5. Retorna: { user_id, role, scopes, updated_at }
```

### Restricción importante
Un admin NO puede quitarse el scope `administrador` a sí mismo. Evita que el CDA quede sin administrador.

---

## UC-11 — Desactivar Trabajador

**Actor:** Usuario con scope `administrador`

### Flujo principal
```
1. Admin envía: DELETE /users/workers/{user_id}
2. Sistema verifica scope 'administrador' en JWT
3. Sistema verifica que el user NO sea el mismo caller
4. Sistema actualiza trust_user_cda: active = false
   (NO elimina de Cognito, NO elimina de trust_users)
   (El trabajador puede seguir en otros CDAs)
5. Retorna: 200 — "Trabajador desactivado"
```

### Nota de diseño
Nunca se elimina un usuario del sistema. Solo se desactiva la relación con el CDA. Si el trabajador vuelve, el admin lo reactiva.

---

## UC-12 — Validar Token (uso interno)

**Actor:** Otros microservicios (quality-service, user-service, etc.)  
**Trigger:** Un microservicio recibe una petición y necesita validar el JWT

### Flujo principal
```
1. Microservicio envía: GET /internal/validate-token
   Headers: Authorization: Bearer {jwt}
            X-Internal-Key: {clave compartida entre servicios}
2. Sistema verifica firma del JWT
3. Sistema verifica que el token no esté en blacklist
4. Sistema retorna claims: { user_id, cda_id, role, scopes[], valid: true }
```

### Alternativa recomendada
En lugar de llamar a este endpoint, cada microservicio puede validar el JWT localmente usando la clave pública compartida (más eficiente, sin red hop). El endpoint existe como fallback y para verificar blacklist.

---

## Resumen de casos de uso por implementar

| # | Caso de uso | Prioridad | Complejidad | Scope requerido |
|---|------------|-----------|-------------|-----------------|
| UC-01 | Login | 🔴 Alta | Media | Público |
| UC-02 | Refresh Token | 🔴 Alta | Baja | Público |
| UC-03 | Logout | 🟡 Media | Baja | Autenticado |
| UC-04 | Activar CDA | 🔴 Alta | Alta | Interno Trust |
| UC-05 | Completar Perfil | 🔴 Alta | Media | Público (con session_token) |
| UC-06 | Olvidé Contraseña | 🟡 Media | Baja | Público |
| UC-07 | Confirmar Reset | 🟡 Media | Baja | Público |
| UC-08 | Crear Trabajador | 🔴 Alta | Alta | administrador |
| UC-09 | Listar Trabajadores | 🟡 Media | Baja | administrador |
| UC-10 | Actualizar Trabajador | 🟡 Media | Baja | administrador |
| UC-11 | Desactivar Trabajador | 🟡 Media | Baja | administrador |
| UC-12 | Validar Token | 🔴 Alta | Baja | Interno |

### Orden de implementación sugerido
```
Sprint 1: UC-04 → UC-05 → UC-01 → UC-02  (base del sistema)
Sprint 2: UC-08 → UC-09 → UC-11          (gestión de usuarios)
Sprint 3: UC-03 → UC-06 → UC-07 → UC-10  (complementarios)
Sprint 4: UC-12                           (integración entre servicios)
```
