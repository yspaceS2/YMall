# 로컬 문제 해결

## 공통 확인

```powershell
docker compose ps
docker compose logs --tail=200 backend frontend
docker info
```

Docker Desktop 실행 여부와 컨테이너의 `healthy` 상태를 먼저 확인합니다.

## PostgreSQL `localhost:5432 refused`

IntelliJ에서 Backend만 실행할 때 DB 컨테이너가 없거나 호스트 포트가 열리지 않으면 발생합니다.

```powershell
docker compose up -d postgres redis kafka
docker compose ps postgres
Test-NetConnection localhost -Port 5432
```

IDE 실행은 `jdbc:postgresql://localhost:5432/...`, Compose 내부 Backend는 서비스 이름 `postgres`를 사용합니다. 실행 위치에 따라 Host가 다릅니다.

## Flyway 시작 실패

DB 연결을 먼저 해결하고 migration history를 확인합니다. 기존 migration의 checksum을 맞추려고 파일을 수정하지 않습니다. Volume 삭제는 데이터를 제거하므로 백업 여부를 확인한 뒤에만 수행합니다. 자세한 기준은 [Flyway 이력 관리](flyway-migration-history.md)를 참고합니다.

## Frontend API 호출 실패

- 전체 Compose: `http://localhost:5173`의 Nginx가 `/api`를 Backend로 전달합니다.
- VS Code 개발 서버: `frontend`에서 `npm run dev`를 실행하고 Vite proxy와 Backend 주소를 확인합니다.
- CORS 오류: Backend 허용 Origin과 실제 Frontend 주소가 일치해야 합니다.

```powershell
curl http://localhost:5173/health
curl "http://localhost:5173/api/products?page=0&size=1"
```

## 환경변수가 반영되지 않음

IntelliJ와 VS Code는 시작 시 사용자 환경변수를 읽으므로 완전히 재시작합니다. Compose의 루트 `.env`를 바꿨다면 컨테이너를 다시 생성합니다. `VITE_` 값은 공개되므로 Secret을 넣지 않습니다.

```powershell
docker compose up -d --build --force-recreate backend frontend
```

## 포트 충돌

```powershell
Get-NetTCPConnection -State Listen | Where-Object LocalPort -In 5173,5432
```

기존 프로세스를 확인해 종료하거나 로컬 포트를 바꿉니다. PostgreSQL·Redis·Kafka는 전체 Compose 실행에서 외부 공개가 필요하지 않습니다. 추가 실행·백업 절차는 [Docker Compose 가이드](docker-compose.md)를 참고합니다.
