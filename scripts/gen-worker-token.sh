#!/bin/bash
# Genera un JWT de prueba con scope ADMINISTRADOR y un cdaId, para probar /users/workers.
# Uso: ./scripts/gen-worker-token.sh <cdaId>
# SOLO para testing — en producción el token lo emite el login (UC-01).
set -e

REGION="us-east-1"
CDA_ID="${1:?Uso: ./scripts/gen-worker-token.sh <cdaId>}"

SECRET=$(aws ssm get-parameter \
  --name "/trust/jwt/secret" \
  --with-decryption \
  --query 'Parameter.Value' \
  --output text \
  --region "$REGION")

HEADER='{"alg":"HS256","typ":"JWT"}'
EXP=$(($(date +%s) + 3600))
PAYLOAD="{\"sub\":\"admin-test\",\"email\":\"admin@trust.com.co\",\"cda_id\":\"$CDA_ID\",\"scopes\":[\"ADMINISTRADOR\"],\"exp\":$EXP}"

b64() { openssl base64 -e -A | tr '+/' '-_' | tr -d '='; }

H=$(printf '%s' "$HEADER"  | b64)
P=$(printf '%s' "$PAYLOAD" | b64)
SIG=$(printf '%s' "$H.$P" | openssl dgst -sha256 -hmac "$SECRET" -binary | b64)

echo "$H.$P.$SIG"
