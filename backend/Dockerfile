# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy@sha256:29467857e8bde40ab1f7befecbda0ea764b95afec1cc7f89aa90f7a766577e19 AS builder

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy@sha256:89e68b9bb83713510b63e2059a415792a7fc77e14b739a7d7ede97f6d9ca2c38 AS runtime

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system ymall \
    && useradd --system --gid ymall --home-dir /app --shell /usr/sbin/nologin ymall

WORKDIR /app

COPY --from=builder --chown=ymall:ymall /workspace/build/libs/backend-*.jar app.jar
RUN mkdir -p /app/uploads && chown -R ymall:ymall /app/uploads

USER ymall

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=UTC \
    JAVA_TOOL_OPTIONS=-Duser.timezone=UTC

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl --fail --silent http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
