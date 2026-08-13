#!/usr/bin/env bash

set -Eeuo pipefail

readonly PROJECT_DIR="${YMALL_PROJECT_DIR:-/opt/ymall}"

read_project_env() {
    local key="$1"

    [[ -f "${PROJECT_DIR}/.env" ]] || return 0
    awk -F= -v key="${key}" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "${PROJECT_DIR}/.env"
}

readonly BACKUP_ROOT="${YMALL_BACKUP_ROOT:-/opt/ymall-backups}"
readonly RETENTION_DAYS="${YMALL_BACKUP_RETENTION_DAYS:-7}"
readonly UPLOAD_VOLUME="${YMALL_UPLOAD_VOLUME:-ymall_backend-uploads}"
readonly LOCK_FILE="${YMALL_BACKUP_LOCK_FILE:-/run/lock/ymall-backup.lock}"
readonly OCI_NAMESPACE="${YMALL_OCI_BACKUP_NAMESPACE:-$(read_project_env YMALL_OCI_BACKUP_NAMESPACE)}"
readonly OCI_BUCKET="${YMALL_OCI_BACKUP_BUCKET:-$(read_project_env YMALL_OCI_BACKUP_BUCKET)}"
readonly TIMESTAMP="$(TZ=Asia/Seoul date +%Y%m%d%H%M%S)"
readonly FINAL_DIR="${BACKUP_ROOT}/${TIMESTAMP}"
readonly PARTIAL_DIR="${FINAL_DIR}.partial"
readonly -a COMPOSE=(
    docker compose
    --env-file "${PROJECT_DIR}/.env"
    -f "${PROJECT_DIR}/compose.yaml"
    -f "${PROJECT_DIR}/compose.prod.yaml"
)

validate_configuration() {
    if [[ ! "${RETENTION_DAYS}" =~ ^[1-9][0-9]*$ ]]; then
        echo "YMALL_BACKUP_RETENTION_DAYS must be a positive integer." >&2
        exit 1
    fi

    local resolved_root
    resolved_root="$(readlink -m "${BACKUP_ROOT}")"
    case "${resolved_root}" in
        /|/opt|/opt/ymall|/home|/root)
            echo "Refusing unsafe backup root: ${resolved_root}" >&2
            exit 1
            ;;
    esac

    [[ -f "${PROJECT_DIR}/.env" ]] || {
        echo "Missing production environment file: ${PROJECT_DIR}/.env" >&2
        exit 1
    }
    command -v oci > /dev/null 2>&1 || {
        echo "OCI CLI is required for external backup replication." >&2
        exit 1
    }
    [[ -n "${OCI_NAMESPACE}" && -n "${OCI_BUCKET}" ]] || {
        echo "YMALL_OCI_BACKUP_NAMESPACE and YMALL_OCI_BACKUP_BUCKET are required." >&2
        exit 1
    }
}

cleanup_partial() {
    if [[ -d "${PARTIAL_DIR}" ]]; then
        rm -rf -- "${PARTIAL_DIR}"
    fi
}

prune_expired_backups() {
    find "${BACKUP_ROOT}" \
        -mindepth 1 \
        -maxdepth 1 \
        -type d \
        -name '20????????????' \
        -mtime "+$((RETENTION_DAYS - 1))" \
        -exec rm -rf -- {} +
}

upload_to_object_storage() {
    local file_name

    echo "Replicating backup ${TIMESTAMP} to OCI Object Storage."
    for file_name in database.dump uploads.tar.gz SHA256SUMS; do
        oci os object put \
            --auth instance_principal \
            --namespace-name "${OCI_NAMESPACE}" \
            --bucket-name "${OCI_BUCKET}" \
            --name "${TIMESTAMP}/${file_name}" \
            --file "${FINAL_DIR}/${file_name}" \
            --force \
            --no-multipart > /dev/null
        oci os object head \
            --auth instance_principal \
            --namespace-name "${OCI_NAMESPACE}" \
            --bucket-name "${OCI_BUCKET}" \
            --name "${TIMESTAMP}/${file_name}" > /dev/null
    done
    echo "OCI Object Storage replication verified: ${TIMESTAMP}/"
}

main() {
    validate_configuration
    install -d -m 0700 "${BACKUP_ROOT}"

    exec 9>"${LOCK_FILE}"
    if ! flock -n 9; then
        echo "Another YMall backup is already running." >&2
        exit 1
    fi

    trap cleanup_partial EXIT
    mkdir -m 0700 "${PARTIAL_DIR}"

    echo "Creating PostgreSQL backup ${TIMESTAMP}."
    "${COMPOSE[@]}" exec -T postgres sh -eu -c \
        'export PGPASSWORD="$DB_BACKUP_PASSWORD"; exec pg_dump --format=custom --compress=6 --no-owner --no-acl --username="$DB_BACKUP_USERNAME" "$POSTGRES_DB"' \
        > "${PARTIAL_DIR}/database.dump"

    echo "Creating upload volume backup ${TIMESTAMP}."
    docker volume inspect "${UPLOAD_VOLUME}" > /dev/null
    docker run --rm \
        --volume "${UPLOAD_VOLUME}:/source:ro" \
        postgres:16-alpine \
        tar -C /source -czf - . \
        > "${PARTIAL_DIR}/uploads.tar.gz"

    (
        cd "${PARTIAL_DIR}"
        sha256sum database.dump uploads.tar.gz > SHA256SUMS
    )

    mv "${PARTIAL_DIR}" "${FINAL_DIR}"
    trap - EXIT
    upload_to_object_storage
    prune_expired_backups

    du -sh "${FINAL_DIR}"
    echo "YMall backup completed: ${FINAL_DIR}"
}

main "$@"
