# React Frontend 컨테이너 실행

## 이미지 빌드

프로젝트 루트에서 Frontend 디렉터리를 빌드 컨텍스트로 지정한다.

```bash
docker build -t ymall-frontend:local ./frontend
```

Vite 공개 설정을 변경해야 할 때만 빌드 인자를 전달한다. 기본값은 같은 출처의 Nginx 프록시를
사용하는 `/api`와 빈 OAuth2 주소다.

```bash
docker build \
  --build-arg VITE_API_BASE_URL=/api \
  --build-arg VITE_OAUTH2_BASE_URL= \
  -t ymall-frontend:local \
  ./frontend
```

`VITE_` 값은 브라우저 번들에 포함되는 공개 설정이므로 시크릿을 넣으면 안 된다.

## 컨테이너 실행

```bash
docker run --rm \
  --name ymall-frontend \
  -p 5173:8080 \
  -e BACKEND_HOST=host.docker.internal \
  -e BACKEND_PORT=8080 \
  ymall-frontend:local
```

Docker Compose에서는 `BACKEND_HOST=backend`를 사용한다. Nginx는 다음 요청을 Backend로 전달한다.

- `/api/`
- `/images/`
- `/oauth2/`
- `/login/oauth2/`

그 외 경로는 React SPA로 제공된다. 직접 URL을 입력하거나 새로고침해도 `index.html`로 연결된다.

## 상태 확인

```bash
docker inspect --format='{{json .State.Health}}' ymall-frontend
curl http://localhost:5173/health
```

런타임 이미지는 8080 포트에서 비루트 사용자로 Nginx를 실행한다. 해시가 포함된 정적 파일은
장기 캐시하고 `index.html`은 캐시하지 않는다.
