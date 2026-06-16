# auth-service

Microservicio de autenticación de **Trust**. Único servicio que habla con AWS Cognito y emite los JWT enriquecidos con el contexto del tenant (CDA).

- Diseño detallado: [ARCHITECTURE.md](ARCHITECTURE.md)
- Casos de uso: [USE_CASES.md](USE_CASES.md)

## Stack

Java 17 · Spring WebFlux (reactivo) · Arquitectura Hexagonal · Gradle
AWS: Cognito · DynamoDB · ECS Fargate · ALB · SSM · ECR
Email: Resend

## Estado

| HU | Estado |
|----|--------|
| HU-04 — Activar CDA (`POST /internal/cdas`) | ✅ Funcionando end-to-end |
| UC-01 — Login | ⏳ Pendiente |
| Bootstrap primer `TRUST_ADMIN` real | ⏳ Pendiente |
| Verificar dominio propio en Resend | ⏳ Pendiente (hoy usa dominio de prueba) |

## Cómo probar la HU-04

El endpoint `POST /internal/cdas` exige un JWT con scope `TRUST_ADMIN`.
Como el login (UC-01) aún no existe, se genera un token de prueba:

```bash
# 1. Generar token de prueba (saca el secret de SSM y lo firma)
TOKEN=$(./scripts/gen-test-token.sh)

# 2. DNS del balanceador
ALB=$(aws elbv2 describe-load-balancers \
  --names trust-auth-service-qa-alb \
  --query 'LoadBalancers[0].DNSName' --output text --region us-east-1)

# 3. Llamar al endpoint (JSON en camelCase)
curl -X POST "http://$ALB/internal/cdas" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CDA de Prueba",
    "companyCode": "TEST-004",
    "adminEmail": "tu-correo-de-resend@gmail.com",
    "adminName": "Prueba"
  }'
```

Notas:
- El `companyCode` debe ser único (si existe → `409`).
- Con el dominio de prueba de Resend, `adminEmail` debe ser **el correo de tu cuenta Resend**.
- El token de prueba expira en 1 hora; si vence, vuelve a correr el script.

## Secrets (AWS SSM Parameter Store)

ECS inyecta estos parámetros al contenedor en runtime. Los de Cognito los
crea Terraform automáticamente; los demás se cargan una vez con `aws ssm put-parameter`.

| Parámetro | Origen |
|-----------|--------|
| `/trust/cognito/user-pool-id` | Terraform |
| `/trust/cognito/client-id` | Terraform |
| `/trust/cognito/client-secret` | Terraform |
| `/trust/resend/api-key` | Manual |
| `/trust/resend/from-email` | Manual (`onboarding@resend.dev` en pruebas) |
| `/trust/jwt/secret` | Manual |
| `/trust/frontend-url` | Manual |

## Despliegue

CI/CD con GitHub Actions (`.github/workflows/deploy.yml`). En cada push a `main`:

```
1. Build & Test (Gradle)
2. Terraform → crea/actualiza infra (VPC, ECR, ECS, ALB, DynamoDB, Cognito, IAM)
3. Deploy → build imagen → push a ECR → despliega en ECS Fargate
```

GitHub Secrets necesarios: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_ACCOUNT_ID`, `AWS_REGION`.

## Logs

CloudWatch — grupo `/ecs/trust-auth-service-qa`. En vivo desde terminal:

```bash
aws logs tail /ecs/trust-auth-service-qa --follow --region us-east-1
```

## Infraestructura

Terraform en [`infra/`](infra/). El estado vive en S3 (`trust-terraform-state-188776115228`).
El pipeline corre `terraform apply`; no se aplica a mano.
