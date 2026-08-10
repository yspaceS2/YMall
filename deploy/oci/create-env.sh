#!/usr/bin/env bash

set -Eeuo pipefail

readonly DEPLOY_DIR="${1:-/home/ubuntu/ymall}"
readonly ENV_FILE="${DEPLOY_DIR}/.env"

umask 077

postgres_password="$(openssl rand -base64 36 | tr -d '\n')"
jwt_secret="$(openssl rand -base64 48 | tr -d '\n')"
settlement_key="$(openssl rand -base64 32 | tr -d '\n')"

cat > "${ENV_FILE}" <<EOF
YMALL_DOMAIN=ymall.cloud
ACME_EMAIL=admin@ymall.cloud
CORS_ALLOWED_ORIGINS=https://ymall.cloud
OAUTH2_FRONTEND_REDIRECT_URI=https://ymall.cloud/oauth2/callback

POSTGRES_DB=ymall
POSTGRES_USER=ymall_user
POSTGRES_PASSWORD=${postgres_password}

JWT_SECRET=${jwt_secret}
SELLER_SETTLEMENT_ACCOUNT_ENCRYPTION_KEY=${settlement_key}

GOOGLE_CLIENT_ID=deployment-smoke-test-disabled
GOOGLE_CLIENT_SECRET=deployment-smoke-test-disabled
KAKAO_CLIENT_ID=deployment-smoke-test-disabled
KAKAO_CLIENT_SECRET=deployment-smoke-test-disabled
NAVER_CLIENT_ID=deployment-smoke-test-disabled
NAVER_CLIENT_SECRET=deployment-smoke-test-disabled

TOSS_CLIENT_KEY=deployment-smoke-test-disabled
TOSS_SECRET_KEY=deployment-smoke-test-disabled

MAIL_HOST=localhost
MAIL_PORT=2525
MAIL_USERNAME=deployment-smoke-test-disabled
MAIL_PASSWORD=deployment-smoke-test-disabled
MAIL_FROM=noreply@ymall.cloud
MAIL_HEALTH_ENABLED=false
EOF

chmod 600 "${ENV_FILE}"

variable_count="$(grep -c '^[A-Z0-9_]*=' "${ENV_FILE}")"
if [[ "$(stat -c '%a' "${ENV_FILE}")" != "600" || "${variable_count}" != "23" ]]; then
    echo "Failed to create a protected deployment environment file." >&2
    exit 1
fi

echo "Environment file created with permission 600 and ${variable_count} variables."
