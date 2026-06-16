#!/bin/bash
# Genera un JWT de prueba con scope TRUST_ADMIN para probar /internal/cdas.
# SOLO para testing — en producción el token lo emite el login (UC-01).
set -e

REGION="us-east-1"

# Saca el secret de SSM (el mismo que usa el servicio para validar)
SECRET=$(aws ssm get-parameter \
  --name "/trust/jwt/secret" \
  --with-decryption \
  --query 'Parameter.Value' \
  --output text \
  --region "$REGION")

HEADER='{"alg":"HS256","typ":"JWT"}'
EXP=$(($(date +%s) + 3600))   # expira en 1 hora
PAYLOAD="{\"sub\":\"trust-admin-test\",\"email\":\"admin@trust.com.co\",\"scopes\":[\"TRUST_ADMIN\"],\"exp\":$EXP}"

b64() { openssl base64 -e -A | tr '+/' '-_' | tr -d '='; }

H=$(printf '%s' "$HEADER"  | b64)
P=$(printf '%s' "$PAYLOAD" | b64)
SIG=$(printf '%s' "$H.$P" | openssl dgst -sha256 -hmac "$SECRET" -binary | b64)

echo "$H.$P.$SIG"
