#!/usr/bin/env bash

set -Eeuo pipefail

readonly PROJECT_DIR="${YMALL_PROJECT_DIR:-/opt/ymall}"
readonly -a COMPOSE=(docker compose --env-file "${PROJECT_DIR}/.env" -f "${PROJECT_DIR}/compose.yaml" -f "${PROJECT_DIR}/compose.prod.yaml")

"${COMPOSE[@]}" exec -T postgres sh -eu -c '
    export PGPASSWORD="$POSTGRES_PASSWORD"
    exec psql --host=localhost --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --file=/opt/ymall/verify-db-roles.sql
'
