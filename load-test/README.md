# YMall k6 부하 테스트

YMALL-74에서 구축한 로컬 부하 테스트 환경입니다. 운영 환경이 아니라 격리된 로컬 또는
테스트 환경에서만 실행합니다.

## 시나리오

| 시나리오 | 대상 | 데이터 변경 |
| --- | --- | --- |
| `read` | 상품 목록, 상세, 검색 | 없음 |
| `mixed` | 로그인, 토큰 갱신, 장바구니·주문 조회 | 기본값은 없음 |
| `spike` | 상품 상세 조회의 순간 트래픽 증가 | 없음 |

`mixed`에서 장바구니 추가와 주문 생성을 실행하려면
`LOAD_TEST_ENABLE_WRITES=true`를 명시해야 합니다.

## 사전 준비

1. `docker compose up -d`로 YMall 컨테이너를 실행합니다.
2. 조회 테스트에는 승인 상태이며 재고가 1개 이상인 상품이 필요합니다.
3. 혼합 테스트에는 목표 VU 수만큼의 전용 일반 회원 계정이 필요합니다.
4. 주문 생성 테스트를 할 회원에는 배송지를 등록하고, 전용 상품에는 충분한 재고를
   준비합니다.
5. 프로젝트 루트의 로컬 `.env`에 필요한 값만 입력합니다. 계정, 비밀번호, 토큰은
   `.env.example`, 문서, 스크립트 또는 Git에 기록하지 않습니다.

여러 계정은 쉼표로 구분하며 이메일과 비밀번호의 순서를 맞춥니다.

```dotenv
LOAD_TEST_USER_EMAILS=load-user-1@example.test,load-user-2@example.test
LOAD_TEST_USER_PASSWORDS=local-secret-1,local-secret-2
```

위 값은 형식 예시이며 실제로 사용할 값은 커밋하지 않는 루트 `.env`에만 둡니다.
한 계정을 여러 VU가 공유하면 refresh token과 장바구니가 충돌할 수 있으므로 스크립트가
계정 수보다 큰 VU 실행을 거부합니다.

## 실행

상품 조회:

```powershell
$env:LOAD_TEST_SCENARIO = "read"
docker compose --profile load-test run --rm k6
```

로그인과 장바구니·주문 조회:

```powershell
$env:LOAD_TEST_SCENARIO = "mixed"
$env:LOAD_TEST_TARGET_VUS = "2"
docker compose --profile load-test run --rm k6
```

실제 장바구니 추가와 주문 생성:

```powershell
$env:LOAD_TEST_SCENARIO = "mixed"
$env:LOAD_TEST_ENABLE_WRITES = "true"
$env:LOAD_TEST_TARGET_VUS = "2"
docker compose --profile load-test run --rm k6
```

순간 부하:

```powershell
$env:LOAD_TEST_SCENARIO = "spike"
docker compose --profile load-test run --rm k6
```

PowerShell 환경 변수가 루트 `.env`보다 우선합니다. 테스트 후에는 현재 터미널을 닫거나
설정한 환경 변수를 제거합니다.

## 주요 설정

| 환경 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `LOAD_TEST_PRODUCT_ID` | 자동 탐색 | 테스트할 승인 상품 ID |
| `LOAD_TEST_TARGET_VUS` | `10` | `read`, `mixed` 목표 VU |
| `LOAD_TEST_RAMP_UP` | `30s` | 목표 VU까지 증가 시간 |
| `LOAD_TEST_HOLD` | `1m` | 목표 VU 유지 시간 |
| `LOAD_TEST_RAMP_DOWN` | `30s` | VU 감소 시간 |
| `LOAD_TEST_SPIKE_BASE_VUS` | `5` | 순간 부하 전후 기준 VU |
| `LOAD_TEST_SPIKE_VUS` | `50` | 순간 최대 VU |
| `LOAD_TEST_SPIKE_DURATION` | `30s` | 순간 최대 부하 유지 시간 |
| `LOAD_TEST_P95_MS` | `500` | 전체 요청 p95 임계값(ms) |
| `LOAD_TEST_FAILURE_RATE` | `0.01` | 허용 HTTP 실패율 |
| `LOAD_TEST_REFRESH_EVERY` | `20` | 혼합 시나리오 토큰 갱신 주기 |
| `LOAD_TEST_WRITE_EVERY` | `10` | 주문 생성 실행 주기 |

각 실행은 HTTP 실패율, p95 응답 시간, check 성공률 임계값을 평가하며 임계값을 넘으면
k6가 0이 아닌 종료 코드로 끝납니다.

## 반복 실행 시 주의

- `read`와 `spike`는 데이터를 변경하지 않아 그대로 반복할 수 있습니다.
- 쓰기가 켜진 `mixed`는 주문마다 재고를 차감합니다. 반복 전 테스트 상품 재고를
  확인하고, 운영 상품이나 실제 회원 계정은 사용하지 않습니다.
- 주문에는 매 요청마다 다른 idempotency key를 사용합니다.
- 주문 생성 실패 시 이번 반복에서 추가한 장바구니 항목은 가능한 경우 삭제합니다.
- 비밀번호나 토큰을 명령행 인자로 직접 쓰면 셸 기록에 남을 수 있으므로 로컬 `.env`를
  사용합니다.
