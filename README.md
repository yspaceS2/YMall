# YMall

YMall은 사용자, 판매자, 관리자의 상품·주문·결제·배송 흐름을 구현한 쇼핑몰 포트폴리오
프로젝트입니다. React Frontend와 Spring Boot Backend를 사용하고 PostgreSQL, Redis, Kafka를
Docker Compose로 함께 실행합니다.

## 기술 구성

- Frontend: React, TypeScript, Vite, Tailwind CSS, Nginx
- Backend: Java 17, Spring Boot, Spring Security, JPA, Flyway
- Data: PostgreSQL, Redis
- Messaging: Kafka, Transactional Outbox
- Infra: Docker, Docker Compose, GitHub Actions

## 가장 빠른 로컬 실행

### 1. 준비 사항

- Git
- Docker Desktop과 Docker Compose
- Windows에서는 Docker Desktop의 WSL 2 기반 엔진
- 사용 가능한 `5173` 포트

Docker Desktop이 실행된 상태에서 저장소를 복제하고 프로젝트 루트로 이동합니다.

```bash
git clone https://github.com/yspaceS2/YMall.git
cd YMall
```

### 2. 환경변수 준비

일반 회원가입과 상품·주문 흐름만 확인할 때는 `.env` 없이도 로컬 기본값으로 실행할 수 있습니다.
OAuth2 로그인 또는 이메일 발송까지 확인하려면 예시 파일을 복사한 후 실제 로컬 값을 입력합니다.

PowerShell:

```powershell
Copy-Item .env.example .env
```

Bash:

```bash
cp .env.example .env
```

`.env`에는 DB 비밀번호, JWT Secret, OAuth Client Secret 등이 들어가므로 Git에 커밋하지 않습니다.
브라우저 번들에 포함되는 `VITE_` 환경변수에도 시크릿을 넣으면 안 됩니다.

### 3. 전체 서비스 실행

```bash
docker compose up -d --build
docker compose ps
```

모든 서비스가 `healthy`가 되면 [http://localhost:5173](http://localhost:5173)으로 접속합니다.
Frontend만 호스트에 공개되고 Backend, PostgreSQL, Redis, Kafka는 Compose 내부 네트워크에서
통신합니다. 새 PostgreSQL 볼륨에는 회원이나 상품 데이터가 없으므로 필요한 데이터는 직접 생성합니다.

### 4. 로그와 상태 확인

```bash
docker compose logs -f backend frontend
docker compose ps
```

```bash
curl http://localhost:5173/health
curl "http://localhost:5173/api/products?page=0&size=1"
```

### 5. 종료

데이터를 보존하면서 컨테이너만 종료합니다.

```bash
docker compose down
```

`docker compose down --volumes`는 PostgreSQL을 포함한 로컬 데이터를 삭제하므로 일반 종료에 사용하지
마세요.

## 상세 문서

- [통합 Docker Compose 실행·백업·문제 해결](docs/docker-compose.md)
- [Backend 컨테이너](docs/backend-container.md)
- [Frontend 컨테이너](docs/frontend-container.md)
- [Kafka 개발 환경](docs/kafka-development.md)
- [Transactional Outbox](docs/transactional-outbox.md)
- [Kafka 재시도와 DLT](docs/kafka-retry-dlt.md)
- [서버·DB·API 시간대 운영 기준](docs/timezone.md)
- [Toss Payments 테스트와 장애 대응](docs/toss-payments.md)
- [결제 취소·환불 운영 가이드](docs/payment-refunds.md)
- [Toss Payments 웹훅 운영 가이드](docs/payment-webhooks.md)

## 로컬 서비스 구성

| 서비스 | 역할 | 호스트 공개 |
| --- | --- | --- |
| Frontend | React 정적 파일과 Nginx API 프록시 | `5173` |
| Backend | Spring Boot API | 공개하지 않음 |
| PostgreSQL | 영속 원장 데이터 | 공개하지 않음 |
| Redis | Refresh Token과 상품 캐시 | 공개하지 않음 |
| Kafka | 주문 이벤트 전달 | 공개하지 않음 |

운영 배포에서는 Compose의 로컬 기본 비밀번호와 Hibernate `update`를 사용하지 않습니다. 운영 프로필은
Hibernate `validate`를 유지하며, 시크릿 저장소와 운영용 초기 Flyway 마이그레이션이 별도로 필요합니다.
