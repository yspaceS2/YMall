#!/usr/bin/env bash

set -Eeuo pipefail

readonly PROJECT_DIR="${YMALL_PROJECT_DIR:-/opt/ymall}"
readonly BACKUP_ROOT="${YMALL_BACKUP_ROOT:-/opt/ymall-backups}"
readonly POSTGRES_CONTAINER="${YMALL_POSTGRES_CONTAINER:-ymall-postgres-1}"
readonly BACKUP_DIR="${1:-$(find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d -name '20????????????' -printf '%p\n' | sort | tail -n 1)}"
readonly VERIFY_SUFFIX="$(date +%Y%m%d%H%M%S)-$$"
readonly VERIFY_DATABASE="ymall_backup_verify_${VERIFY_SUFFIX//-/_}"
readonly CONTAINER_DUMP="/tmp/${VERIFY_DATABASE}.dump"

cleanup() {
    docker exec "${POSTGRES_CONTAINER}" sh -c \
        'dropdb --if-exists --force --username="$POSTGRES_USER" "$1" >/dev/null 2>&1 || true' \
        sh "${VERIFY_DATABASE}" || true
    docker exec "${POSTGRES_CONTAINER}" rm -f "${CONTAINER_DUMP}" >/dev/null 2>&1 || true
}

main() {
    [[ -n "${BACKUP_DIR}" && -d "${BACKUP_DIR}" ]] || {
        echo "Backup directory not found." >&2
        exit 1
    }
    [[ -f "${BACKUP_DIR}/database.dump" ]] || {
        echo "database.dump is missing." >&2
        exit 1
    }
    [[ -f "${BACKUP_DIR}/uploads.tar.gz" ]] || {
        echo "uploads.tar.gz is missing." >&2
        exit 1
    }

    echo "Verifying checksums in ${BACKUP_DIR}."
    (cd "${BACKUP_DIR}" && sha256sum --check SHA256SUMS)
    tar -tzf "${BACKUP_DIR}/uploads.tar.gz" > /dev/null

    trap cleanup EXIT
    docker cp "${BACKUP_DIR}/database.dump" "${POSTGRES_CONTAINER}:${CONTAINER_DUMP}"
    docker exec "${POSTGRES_CONTAINER}" sh -c \
        'createdb --username="$POSTGRES_USER" "$1"' \
        sh "${VERIFY_DATABASE}"
    docker exec "${POSTGRES_CONTAINER}" sh -c \
        'pg_restore --exit-on-error --no-owner --no-acl --username="$POSTGRES_USER" --dbname="$1" "$2"' \
        sh "${VERIFY_DATABASE}" "${CONTAINER_DUMP}"

    local table_count
    table_count="$(docker exec "${POSTGRES_CONTAINER}" sh -c \
        'psql --tuples-only --no-align --username="$POSTGRES_USER" --dbname="$1" --command="SELECT count(*) FROM information_schema.tables WHERE table_schema = '\''public'\'';"' \
        sh "${VERIFY_DATABASE}")"
    [[ "${table_count}" =~ ^[1-9][0-9]*$ ]] || {
        echo "Restored verification database contains no public tables." >&2
        exit 1
    }

    echo "Backup verification completed: ${table_count} public tables restored."
}

main "$@"
