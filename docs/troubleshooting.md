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

## 운영 OAuth 로그인 후 다시 로그인 화면으로 돌아옴

Google·Naver·Kakao 개발자 콘솔의 Redirect URI와 Backend가 생성하는 Callback 주소가 정확히 일치하는지 확인합니다. 운영 환경은 `https://ymall.cloud/login/oauth2/code/{provider}`를 사용하며 `http`, `https`, `www` 유무와 경로가 하나라도 다르면 공급자가 요청을 거부합니다.

공급자 인증은 성공했지만 Frontend Callback에서 로그인 상태가 유지되지 않으면 다음 순서로 확인합니다.

1. Backend OAuth 성공 로그에서 Token 값이 아닌 성공 여부와 Provider만 확인합니다.
2. Frontend Callback 주소와 운영 Origin 설정을 확인합니다.
3. Refresh Cookie의 Domain, Secure, SameSite 정책과 `/api/members/tokens/refresh` 응답을 확인합니다.
4. 브라우저 확장 기능의 영향을 배제하려면 별도 브라우저나 시크릿 창에서 다시 확인합니다.

Access Token, Refresh Token과 OAuth client secret은 브라우저 화면, 로그나 Jira에 복사하지 않습니다.

## Toss 결제창이 CSP 오류로 열리지 않음

브라우저 Console에서 `connect-src` 또는 `frame-src` 차단 도메인을 확인합니다. Toss Payments SDK가 사용하는 API Gateway와 로그·이벤트 도메인은 운영 Caddy CSP의 허용 목록에 있어야 합니다. 오류를 해결하기 위해 `*`나 전체 `https:`를 허용하지 않고 실제 SDK가 요청한 Toss 도메인만 추가합니다.

키 오류 메시지가 함께 표시되면 결제위젯 연동 키가 아니라 현재 연동 방식에 맞는 API 개별 연동 Client Key인지 확인합니다. Secret Key는 Backend에만 두고 `VITE_` 환경변수로 노출하지 않습니다.

## DB 복원 후 상품 이미지만 보이지 않음

PostgreSQL에는 이미지 경로와 메타데이터가 저장되고 파일 본문은 `backend-uploads` Docker 볼륨에 저장됩니다. DB dump만 복원하면 상품 데이터는 조회되지만 경로가 가리키는 파일이 없어 이미지가 표시되지 않습니다.

같은 시각에 생성한 DB dump와 `uploads.tar.gz`를 함께 복원하고 체크섬을 검증합니다. 운영 복원은 기존 데이터를 교체하는 작업이므로 자동 실행하지 않으며 [OCI 배포와 복구 가이드](deployment/oci.md)의 순서와 대상 백업 시각을 확인한 뒤 수행합니다.
