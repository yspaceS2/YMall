# syntax=docker/dockerfile:1

FROM node:22-alpine@sha256:16e22a550f3863206a3f701448c45f7912c6896a62de43add43bb9c86130c3e2 AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci

COPY . .

ARG VITE_API_BASE_URL=/api
ARG VITE_OAUTH2_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_OAUTH2_BASE_URL=${VITE_OAUTH2_BASE_URL}

RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine@sha256:65e3e85dbaed8ba248841d9d58a899b6197106c23cb0ff1a132b7bfe0547e4c0 AS runtime

COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/proxy_params /etc/nginx/proxy_params
COPY --from=builder --chown=101:101 /app/dist /usr/share/nginx/html

USER 101

EXPOSE 8080

ENV BACKEND_HOST=backend
ENV BACKEND_PORT=8080
ENV NGINX_ENVSUBST_FILTER=^(BACKEND_HOST|BACKEND_PORT)$

HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/health || exit 1
