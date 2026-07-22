# YMall 로컬 Docker Compose 실행

## 실행

프로젝트 루트에서 다음 명령으로 Frontend, Backend, PostgreSQL, Redis, Kafka를 함께 실행한다.

```bash
docker compose up -d --build
docker compose ps
```

브라우저에서는 `http://localhost:5173`으로 접속한다. 호스트에는 Frontend 포트만 공개하며,
Backend와 PostgreSQL, Redis, Kafka는 Compose 내부 네트워크에서 서비스 이름으로 통신한다.

OAuth2 로그인이나 이메일 발송을 검증하려면 `.env.example`을 `.env`로 복사하고 실제 로컬 값을
입력한다. `.env`는 Git에서 제외되며 `VITE_` 공개 설정과 달리 시크릿을 저장할 수 있다.

```bash
cp .env.example .env
```

일반 회원가입, 상품, 장바구니, 주문과 같은 OAuth2·메일 외 기능은 별도 `.env` 없이도 기동할 수
있도록 로컬 기본값을 제공한다. 기본 비밀번호와 JWT 값은 로컬 개발 전용이며 운영에 사용하면 안 된다.
빈 PostgreSQL에서도 로컬 환경을 바로 실행할 수 있도록 Compose의 Backend에만 Hibernate `update`를
적용한다. 운영 프로필 자체는 `validate`를 유지하므로 운영 배포에서는 완전한 초기 마이그레이션을 별도로
준비해야 한다.

## 상태와 로그 확인

```bash
docker compose ps
docker compose logs -f backend frontend
curl http://localhost:5173/health
```

Backend는 PostgreSQL, Redis, Kafka의 Health Check가 통과한 후 시작하고 Frontend는 Backend가
준비된 후 시작한다.

## 종료와 데이터 보존

컨테이너만 종료하면 PostgreSQL, Redis, Kafka와 업로드 데이터가 볼륨에 보존된다.

```bash
docker compose down
docker compose up -d
```

로컬 데이터를 모두 초기화할 때만 볼륨을 함께 삭제한다. 이 명령은 복구할 수 없으므로 필요한 데이터를
백업한 뒤 실행한다.

```bash
docker compose down --volumes
```
