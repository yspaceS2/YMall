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

## IntelliJ와 Vite를 사용하는 개발 모드

평소 코드 개발에서는 PostgreSQL, Redis, Kafka만 Docker로 실행하고 Backend는 IntelliJ,
Frontend는 Vite로 실행할 수 있다. `compose.dev.yaml`은 PostgreSQL과 Kafka를 호스트의
`localhost`에 공개하며, Redis는 기본 `compose.yaml`의 호스트 포트 설정을 그대로 사용한다.

전체 Docker 환경이 실행 중이라면 먼저 컨테이너 Backend와 Frontend를 중지한다. 이 명령은
데이터 볼륨을 삭제하지 않는다.

```bash
docker compose stop backend frontend
```

개발용 설정을 합쳐 인프라만 실행한다.

```bash
docker compose -f compose.yaml -f compose.dev.yaml up -d postgres redis kafka mailhog
docker compose -f compose.yaml -f compose.dev.yaml ps postgres redis kafka mailhog
```

기본 연결 주소는 다음과 같다.

| 서비스 | 로컬 연결 주소 |
| --- | --- |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |
| Kafka | `localhost:9092` |
| MailHog SMTP | `localhost:1025` |
| MailHog Web UI | `http://localhost:8025` |

MailHog는 로컬 이메일 발송을 확인하기 위한 개발 전용 서비스다. 비밀번호 재설정이나 OAuth 이메일 인증을
요청한 뒤 Web UI에서 메일과 인증번호를 확인할 수 있다. 메일은 메모리에만 보관되며 운영 배포에는
MailHog를 포함하지 않고 실제 SMTP 제공자를 사용한다.

IntelliJ에서는 `local` 프로필로 Backend를 실행하고, VS Code 또는 터미널에서는 Vite를 실행한다.
`application-local.yaml`의 PostgreSQL 사용자·비밀번호·DB 이름은 루트 `.env`의
`POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`와 일치해야 한다.

```bash
cd frontend
npm run dev
```

이 구성에서는 Backend와 Frontend 소스 변경을 위해 Docker 이미지를 다시 빌드하지 않아도 된다.
Dockerfile, Compose 설정 또는 실제 컨테이너 통합 동작을 확인할 때는 전체 Docker 환경을 다시
빌드한다.

```bash
docker compose up -d --build
```

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

삭제되는 대상은 `postgres-data`, `redis-data`, `kafka-data`, `backend-uploads` 볼륨이다. 백업이 없다면
PostgreSQL 데이터와 업로드 파일은 복구할 수 없다. Redis 캐시와 Kafka 이벤트는 PostgreSQL 원장을
대체하지 않으므로 운영 데이터의 기준으로 사용하지 않는다.

## PostgreSQL 백업과 복원

백업 디렉터리는 Git에서 제외된다. PowerShell에서는 먼저 디렉터리를 만든다.

```powershell
New-Item -ItemType Directory -Force backups
```

PostgreSQL 컨테이너 안에서 custom format 백업을 만들고 호스트로 복사한다.

```bash
docker compose exec -T postgres pg_dump \
  -U ymall_user \
  -d ymall \
  --format=custom \
  --file=/tmp/ymall.dump
docker compose cp postgres:/tmp/ymall.dump ./backups/ymall.dump
docker compose exec -T postgres rm /tmp/ymall.dump
```

`.env`에서 `POSTGRES_USER`나 `POSTGRES_DB`를 변경했다면 명령의 사용자와 DB 이름도 같은 값으로
변경한다.

복원 중에는 애플리케이션의 DB 접근을 막는다. 아래 명령은 현재 DB 객체를 백업 내용으로 교체한다.

```bash
docker compose stop frontend backend
docker compose cp ./backups/ymall.dump postgres:/tmp/ymall.dump
docker compose exec -T postgres pg_restore \
  -U ymall_user \
  -d ymall \
  --clean \
  --if-exists \
  /tmp/ymall.dump
docker compose exec -T postgres rm /tmp/ymall.dump
docker compose up -d backend frontend
```

복원 전에 현재 데이터가 필요하면 반드시 별도 백업을 만든다. 백업 파일이 없으면 볼륨 삭제 전 상태로
되돌릴 수 없다.

## 업로드 파일 백업과 복원

상품 이미지 등 업로드 파일은 `backend-uploads` 볼륨에 저장된다. PostgreSQL 백업에는 파일 본문이
포함되지 않으므로 함께 백업한다.

```bash
docker run --rm \
  -v ymall_backend-uploads:/source:ro \
  -v "${PWD}/backups:/backup" \
  alpine:3.22 tar czf /backup/backend-uploads.tar.gz -C /source .
```

PowerShell에서 `${PWD}` 마운트가 정상 해석되지 않으면 절대 경로를 사용한다. 복원은 대상 볼륨의 기존
파일을 교체하므로 Backend를 먼저 중지한다.

```bash
docker compose stop backend frontend
docker run --rm \
  -v ymall_backend-uploads:/target \
  -v "${PWD}/backups:/backup:ro" \
  alpine:3.22 sh -c "find /target -mindepth 1 -delete && tar xzf /backup/backend-uploads.tar.gz -C /target"
docker compose up -d backend frontend
```

## 자주 발생하는 문제

### Docker API 또는 named pipe에 연결할 수 없음

`dockerDesktopLinuxEngine` 또는 `docker_engine` named pipe 오류는 보통 Docker Desktop이 종료된
상태라는 의미다. Docker Desktop을 실행하고 엔진 준비가 끝난 뒤 확인한다.

```powershell
docker version
docker compose version
```

### `5173` 포트가 이미 사용 중

실행 중인 개발 서버를 종료하거나 `.env`의 `FRONTEND_PORT`를 다른 값으로 변경한다.

```powershell
Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
```

```env
FRONTEND_PORT=5174
```

포트를 변경하면 OAuth 공급자에 등록한 Callback URL과 Redirect URI도 같은 포트로 수정해야 한다.

### Backend가 `unhealthy` 상태

의존 서비스와 Backend 로그를 순서대로 확인한다.

```bash
docker compose ps
docker compose logs --tail=200 postgres redis kafka backend
```

`.env`를 만들었다면 빈 값, DB 이름과 사용자 불일치, 너무 짧은 JWT Secret을 먼저 확인한다. 설정을
변경한 뒤에는 Backend를 다시 생성한다.

```bash
docker compose up -d --force-recreate backend frontend
```

### OAuth2 로그인이 공급자 화면에서 실패

로컬 Callback URL은 다음과 같다.

- Google: `http://localhost:5173/login/oauth2/code/google`
- Naver: `http://localhost:5173/login/oauth2/code/naver`
- Kakao: `http://localhost:5173/login/oauth2/code/kakao`

공급자 콘솔의 URI, `.env`의 Client ID·Secret, 현재 `FRONTEND_PORT`가 서로 일치해야 한다. OAuth2를
사용하지 않는 로컬 흐름에는 공급자 키가 필요하지 않다.

### 컨테이너에서 호스트 서비스에 연결할 수 없음

컨테이너 안의 `localhost`는 호스트 PC가 아니라 해당 컨테이너 자신이다. Docker Desktop에서 호스트의
SMTP 같은 서비스에 접근할 때는 `host.docker.internal`을 사용한다. PostgreSQL, Redis, Kafka는 각각
Compose 서비스 이름 `postgres`, `redis`, `kafka`로 접근한다.

### Kafka 재기동 직후 `NOT_COORDINATOR` 메시지

단일 Broker가 재기동되며 소비자 그룹 Coordinator를 다시 선택하는 동안 일시적으로 나타날 수 있다.
서비스가 `healthy`가 된 뒤에도 계속 반복되는 경우 Kafka와 Backend 로그, 소비자 그룹을 확인한다.

```bash
docker compose exec -T kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 \
  --list
```

### `vmmemWSL` 메모리 사용량이 큼

Docker Desktop의 Linux 컨테이너는 WSL 2 VM 안에서 실행되므로 `vmmemWSL`에 메모리가 표시된다.
먼저 사용하지 않는 컨테이너를 종료한다.

```bash
docker compose down
```

모든 WSL 작업을 종료해도 될 때만 PowerShell에서 `wsl --shutdown`을 사용한다. 지속적으로 제한이
필요하면 사용자 홈의 `.wslconfig`에 메모리와 Swap 상한을 설정하고 WSL을 재시작한다.

```ini
[wsl2]
memory=6GB
swap=2GB
```

메모리 값은 PC 전체 RAM과 동시에 실행할 IDE·브라우저를 고려해 조정한다.

## 전체 초기화 체크리스트

1. 필요한 PostgreSQL과 업로드 파일 백업을 생성한다.
2. 백업 파일이 호스트의 `backups` 디렉터리에 있는지 확인한다.
3. `docker compose down --volumes`로 컨테이너·네트워크·볼륨을 삭제한다.
4. `docker compose up -d --build`로 새 환경을 만든다.
5. `docker compose ps`와 `/health` 응답을 확인한다.

볼륨 삭제 명령은 마지막 확인 후 사용하며, 삭제된 로컬 데이터는 백업 없이는 복구할 수 없다.
