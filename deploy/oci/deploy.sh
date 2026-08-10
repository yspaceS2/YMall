#!/usr/bin/env bash

set -Eeuo pipefail

readonly DEPLOY_BRANCH="main"
readonly HEALTH_URL="https://ymall.cloud/health"
readonly PREVIOUS_COMMIT="$(git rev-parse HEAD)"
readonly -a COMPOSE=(docker compose -f compose.yaml -f compose.prod.yaml)

rollback() {
    local exit_code=$?
    trap - ERR

    echo "Deployment failed. Rolling back to ${PREVIOUS_COMMIT}." >&2
    git reset --hard "${PREVIOUS_COMMIT}"
    "${COMPOSE[@]}" up -d --build --remove-orphans
    exit "${exit_code}"
}

trap rollback ERR

git fetch origin "${DEPLOY_BRANCH}"
git checkout "${DEPLOY_BRANCH}"
git reset --hard "origin/${DEPLOY_BRANCH}"

"${COMPOSE[@]}" config --quiet
"${COMPOSE[@]}" up -d --build --remove-orphans
"${COMPOSE[@]}" ps

curl \
    --fail \
    --silent \
    --show-error \
    --retry 12 \
    --retry-delay 5 \
    "${HEALTH_URL}"

trap - ERR
echo "Deployment completed at $(git rev-parse HEAD)."
