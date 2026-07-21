# Spring Boot Backend 컨테이너 실행

## 이미지 빌드

프로젝트 루트에서 Backend 디렉터리를 빌드 컨텍스트로 지정한다.

```bash
docker build -t ymall-backend:local ./backend
```

멀티 스테이지 빌드가 Gradle로 실행 JAR을 만든 뒤 Java 17 JRE 이미지에 필요한 결과물만 복사한다.
로컬 설정, 테스트 소스, 빌드 결과물과 업로드 파일은 `.dockerignore`로 이미지에서 제외한다.

## 필수 환경변수

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `KAFKA_BOOTSTRAP_SERVERS`
- `JWT_SECRET`
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`
- `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- `OAUTH2_FRONTEND_REDIRECT_URI`
- `CORS_ALLOWED_ORIGINS`

실제 값은 Docker 명령에 직접 기록하지 않고 별도의 `.env` 또는 배포 환경의 Secret 저장소에서 주입한다.
`.env` 파일은 Git에 커밋하지 않는다.

## 컨테이너 실행 예시

```bash
docker run --rm \
  --name ymall-backend \
  --env-file .env \
  -p 8080:8080 \
  -v ymall-uploads:/app/uploads \
  ymall-backend:local
```

컨테이너 안에서 `localhost`는 컨테이너 자신을 의미한다. 호스트 PC에서 실행 중인 PostgreSQL,
Redis 또는 Kafka에 연결할 때 Docker Desktop에서는 환경변수의 호스트를 `host.docker.internal`로 지정한다.

## 상태 확인

```bash
docker inspect --format='{{json .State.Health}}' ymall-backend
curl http://localhost:8080/actuator/health
```

컨테이너는 애플리케이션 계정인 `ymall` 비루트 사용자로 실행된다. Dockerfile의 health check는
`/actuator/health/readiness`를 호출한다.
