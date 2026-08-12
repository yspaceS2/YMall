#!/usr/bin/env bash

set -Eeuo pipefail

readonly DEPLOY_BRANCH="main"
readonly FRONTEND_HEALTH_URL="https://ymall.cloud/health"
readonly BACKEND_HEALTH_URL="https://ymall.cloud/actuator/health"
readonly DEFAULT_AI_MODEL="hf.co/Qwen/Qwen3-4B-GGUF:Q4_K_M"
readonly PREVIOUS_COMMIT="$(git rev-parse HEAD)"
readonly -a COMPOSE=(docker compose -f compose.yaml -f compose.prod.yaml)
deployment_started=false

read_deploy_env() {
    local name="$1"

    [[ -f .env ]] || return
    sed -n "s/^${name}=//p" .env | tail -n 1 | tr -d '\r'
}

check_ai_runtime() {
    local enabled="${AI_REVIEW_ENABLED:-}"
    local required_model="${AI_REVIEW_MODEL:-}"

    [[ -n "${enabled}" ]] || enabled="$(read_deploy_env AI_REVIEW_ENABLED)"
    [[ -n "${required_model}" ]] || required_model="$(read_deploy_env AI_REVIEW_MODEL)"
    enabled="${enabled:-true}"
    required_model="${required_model:-${DEFAULT_AI_MODEL}}"

    if [[ "${enabled}" != "true" ]]; then
        echo "AI review summary is disabled; skipping Model Runner preflight."
        return
    fi

    docker model status > /dev/null
    if ! docker model inspect "${required_model}" > /dev/null 2>&1; then
        echo "Required AI model is not available: ${required_model}" >&2
        echo "Pull it with: docker model pull ${required_model}" >&2
        return 1
    fi

    echo "AI preflight passed: Model Runner is running and the required model is available."
}

recreate_caddy() {
    "${COMPOSE[@]}" up -d --force-recreate --no-deps caddy
}

rollback() {
    local exit_code=$?
    trap - ERR

    if [[ "${deployment_started}" != "true" ]]; then
        echo "Deployment preflight failed before containers were changed." >&2
        git reset --hard "${PREVIOUS_COMMIT}"
        exit "${exit_code}"
    fi

    echo "Deployment failed. Rolling back to ${PREVIOUS_COMMIT}." >&2
    git reset --hard "${PREVIOUS_COMMIT}"
    "${COMPOSE[@]}" up -d --build --remove-orphans
    recreate_caddy
    exit "${exit_code}"
}

trap rollback ERR

git fetch origin "${DEPLOY_BRANCH}"
git checkout "${DEPLOY_BRANCH}"
git reset --hard "origin/${DEPLOY_BRANCH}"

check_ai_runtime
"${COMPOSE[@]}" config --quiet
deployment_started=true
"${COMPOSE[@]}" up -d --build --remove-orphans
recreate_caddy
"${COMPOSE[@]}" ps

for health_url in "${FRONTEND_HEALTH_URL}" "${BACKEND_HEALTH_URL}"; do
    curl \
        --fail \
        --silent \
        --show-error \
        --retry 12 \
        --retry-delay 5 \
        "${health_url}"
done

trap - ERR
echo "Deployment completed at $(git rev-parse HEAD)."
