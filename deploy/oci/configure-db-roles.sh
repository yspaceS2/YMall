#!/usr/bin/env bash

set -Eeuo pipefail

readonly PROJECT_DIR="${YMALL_PROJECT_DIR:-/opt/ymall}"
readonly -a COMPOSE=(docker compose --env-file "${PROJECT_DIR}/.env" -f "${PROJECT_DIR}/compose.yaml" -f "${PROJECT_DIR}/compose.prod.yaml")

"${COMPOSE[@]}" exec -T postgres sh -eu -c '
    for variable in POSTGRES_USER POSTGRES_PASSWORD POSTGRES_DB DB_MIGRATION_USERNAME DB_MIGRATION_PASSWORD DB_APP_USERNAME DB_APP_PASSWORD DB_BACKUP_USERNAME DB_BACKUP_PASSWORD; do
        eval "value=\${$variable:-}"
        if [ -z "$value" ]; then
            echo "Missing required database role variable: $variable" >&2
            exit 1
        fi
    done

    if [ "$POSTGRES_USER" = "$DB_MIGRATION_USERNAME" ] || [ "$POSTGRES_USER" = "$DB_APP_USERNAME" ] || [ "$POSTGRES_USER" = "$DB_BACKUP_USERNAME" ] || [ "$DB_MIGRATION_USERNAME" = "$DB_APP_USERNAME" ] || [ "$DB_MIGRATION_USERNAME" = "$DB_BACKUP_USERNAME" ] || [ "$DB_APP_USERNAME" = "$DB_BACKUP_USERNAME" ]; then
        echo "Database role names must be distinct." >&2
        exit 1
    fi

    export PGPASSWORD="$POSTGRES_PASSWORD"
    exec psql --host=localhost --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --file=/opt/ymall/configure-db-roles.sql
'

echo "PostgreSQL application, migration, and backup roles are configured."
