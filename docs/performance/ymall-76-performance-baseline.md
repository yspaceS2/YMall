# YMALL-76 핵심 사용자 흐름 성능 기준선과 병목 분석

## 결론

로컬 Docker 환경에서 상품 조회, 인증·장바구니·주문 조회, 주문 생성·Kafka 발행
흐름을 분리해 측정했다. 모든 최종 실행은 check 100%, HTTP 실패율 0%로 임계값을
통과했다.

가장 먼저 분석할 구간은 주문 생성의 동기 DB 트랜잭션이다. 주문 포함 시나리오
전체의 p99는 56.58ms였고, 같은 측정 구간의 Prometheus endpoint histogram에서
`POST /api/orders` 16건의 p99는 약 85.22ms로 가장 높았다. 같은 구간에 HikariCP
대기는 0, Kafka Consumer Lag은 0, Outbox 실패도 0이었으므로 DB 연결 풀이나 Kafka
적체보다 주문 생성 트랜잭션 내부의 쿼리와 영속화 작업을 우선 분석한다.

## 측정 환경

| 항목 | 값 |
| --- | --- |
| 측정 시각 | 2026-07-25 23:04~23:20 KST |
| 실행 환경 | Docker Desktop 29.2.1 |
| Docker 자원 | 8 CPU, 약 7.72GiB 메모리 |
| Backend | Spring Boot, Java 17, prod profile |
| 데이터베이스 | PostgreSQL 16 |
| 캐시·메시지 | Redis 7.4, Kafka 3.9.1 |
| 부하 도구 | k6 2.1.0 |
| 측정 전 데이터 | 상품 2개, 리뷰 20개 |

합성 부하 계정만 사용했다. 비밀번호는 실행 시 임의로 생성해 프로세스 메모리에만
보관했으며 원본 결과, 문서, 명령 출력에는 기록하지 않았다.

## 고정 조건

최종 측정 전에 동일 시나리오로 워밍업했다. 각 VU는 요청 묶음 사이에 1초를
대기한다.

| 시나리오 | VU | 단계 | 주요 요청 | 데이터 변경 |
| --- | ---: | --- | --- | --- |
| 상품 조회 | 10 | 30초 증가, 60초 유지, 30초 감소 | 목록, 상세, 검색 | 없음 |
| 인증·회원 조회 | 10 | 30초 증가, 60초 유지, 30초 감소 | 로그인, 토큰 갱신, 장바구니, 주문 목록 | 없음 |
| 주문·Kafka | 2 | 10초 증가, 30초 유지, 10초 감소 | 위 조회 + 5회마다 장바구니 추가·주문 생성 | 주문 16건 |

주문 시나리오는 승인된 합성 상품 ID 2를 사용했다. 재고는 990개에서 974개로
정확히 16개 감소했다.

## k6 기준선

| 시나리오 | 요청 | 처리량 | p50 | p95 | p99 | 최대 | 실패율 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 상품 조회 | 2,692 | 21.84 req/s | 2.20ms | 6.26ms | 7.69ms | 24.84ms | 0% |
| 인증·회원 조회 | 1,851 | 14.93 req/s | 2.39ms | 7.97ms | 12.27ms | 89.67ms | 0% |
| 주문·Kafka | 200 | 3.84 req/s | 4.24ms | 16.83ms | 56.58ms | 87.63ms | 0% |

원본 산출물:

- [상품 조회 JSON](./ymall-76-read-baseline.json)
- [인증·회원 조회 JSON](./ymall-76-mixed-baseline.json)
- [주문·Kafka JSON](./ymall-76-order-kafka-baseline.json)

## 애플리케이션과 인프라 지표

Prometheus는 각 측정 종료 시각을 기준으로 조회했다.

| 지표 | 상품 조회 | 인증·회원 조회 | 주문·Kafka |
| --- | ---: | ---: | ---: |
| Backend 처리량 | 23.56 req/s | 15.24 req/s | 별도 k6 결과 사용 |
| HikariCP 최대 active | 1 | 1 | 짧은 쿼리로 scrape 시점에는 0 |
| HikariCP 최대 pending | 0 | 0 | 0 |
| Redis 최근 1분 적중률 | 100% | 해당 없음 | 해당 없음 |
| Backend CPU | - | - | 약 0.114 core |
| PostgreSQL CPU | - | - | 약 0.020 core |
| Kafka CPU | - | - | 약 0.193 core |
| Backend working set | - | - | 약 575MiB |
| Kafka Consumer Lag | - | - | 전체 partition 0 |

주문·Kafka 측정 시작과 종료 시점의 요청 누적 카운터 차이로 endpoint별 표본 수를
계산하고, 종료 시점의 최근 1분 histogram으로 p99를 확인했다.

| endpoint | 표본 수 | p99 |
| --- | ---: | ---: |
| `POST /api/orders` | 16 | 85.22ms |
| `POST /api/members/login` | 2 | 61.40ms |
| `POST /api/cart/items` | 16 | 38.08ms |
| `GET /api/orders` | 80 | 23.49ms |
| `POST /api/members/tokens/refresh` | 3 | 12.54ms |
| `GET /api/members/me/addresses` | 2 | 10.92ms |
| `GET /api/cart` | 80 | 9.23ms |

주문 생성 endpoint가 가장 높았지만 표본이 16건으로 작으므로 확정적인 성능 보장이
아니라 후속 쿼리 분석의 우선순위를 정하는 근거로만 사용한다. 이후 k6 실행에서는
`order_creation_duration` Trend를 통해 주문 생성 응답 시간을 전체 요청과 분리한다.

`pg_stat_statements`의 누적 통계를 SQL 형태와 대조한 결과, 평균 실행 시간이 가장
긴 반복 쿼리는 애플리케이션 주문 SQL이 아니라 PostgreSQL exporter의 통계 수집
쿼리였다. 현재 지표만으로 특정 주문 SQL을 병목으로 단정할 수 없으므로, 다음
작업에서 부하 측정 직전에 통계를 초기화한 뒤 주문 관련 SQL만 분리하고
`EXPLAIN (ANALYZE, BUFFERS)`로 실행 계획을 확인한다.

## Redis 영향

상품 조회 측정에서 최근 1분 Redis 적중률은 100%였다. endpoint별 Backend p99는
다음과 같다.

| endpoint | p99 |
| --- | ---: |
| 상품 상세 | 3.63ms |
| 상품 검색 | 6.46ms |
| 상품 목록 | 6.87ms |

동일 상품 상세의 캐시 적용 전후를 30회 비교한 기존 서비스 계층 측정에서는
비캐시 평균 4.501ms, 캐시 평균 1.478ms로 약 67.2% 감소했다. 현재 PostgreSQL API
측정에서도 캐시된 상세가 목록·검색보다 약 44~47% 짧아 같은 경향을 보였다.

단, 이번 API 측정은 캐시 비활성화 환경과 동일 조건으로 재실행한 결과가 아니므로
절대 개선율로 사용하지 않는다. 운영과 유사한 데이터 규모에서 캐시 on/off를 직접
비교하는 작업은 후속 개선 항목으로 남긴다.

## Kafka 비동기 처리 영향

주문 16건을 생성하자 Outbox 누계가 54건에서 70건으로 증가했다. 테스트 종료 5초
후 상태는 다음과 같았다.

| 상태 | 건수 |
| --- | ---: |
| PUBLISHED | 70 |
| PENDING | 0 |
| FAILED | 0 |

측정 종료 시 주문 알림 Consumer Lag은 모든 partition에서 0이었다. Outbox 발행률은
약 0.265 event/s였고 재시도·DLT는 발생하지 않았다. 주문 API는 같은 트랜잭션에서
Outbox 행까지만 기록하고 Kafka 발행과 알림 처리는 비동기로 수행하므로, 소비 지연이
주문 응답 시간에 직접 더해지지 않았다.

## 병목과 개선 우선순위

1. **주문 생성 트랜잭션**
   - `POST /api/orders` 16건의 p99가 약 85.22ms로 측정 endpoint 중 가장 높다.
   - Kafka Lag과 HikariCP pending이 0이므로 메시지 적체나 풀 고갈이 원인은 아니다.
   - 주문·주문 항목·재고·Outbox 저장 쿼리 수와 실행 계획을 먼저 분석한다.
2. **상품 목록·검색 DB 조회**
   - 캐시된 상세보다 p99가 약 1.8~1.9배 높다.
   - 상품 수가 2개뿐인 결과이므로 대용량 합성 데이터로 인덱스와 페이지 조회를
     재검증한다.
3. **부하 단계 확장**
   - 현재 CPU와 DB 풀에는 여유가 크다.
   - 10→25→50 VU 단계 측정으로 처리량이 선형 증가하지 않는 지점을 찾아 서버
     사양 산정 근거로 사용한다.

## 재현 명령

전용 계정과 배송지는 루트 `.env`에만 설정한다. 실제 비밀번호나 토큰을 명령행과
문서에 입력하지 않는다.

```powershell
docker compose --profile monitoring up -d --build

$env:LOAD_TEST_SCENARIO = "read"
docker compose --profile load-test run --rm k6 run `
  --summary-export=/results/ymall-76-read-baseline.json `
  /scripts/scenarios/read.js

$env:LOAD_TEST_SCENARIO = "mixed"
$env:LOAD_TEST_TARGET_VUS = "10"
docker compose --profile load-test run --rm k6 run `
  --summary-export=/results/ymall-76-mixed-baseline.json `
  /scripts/scenarios/mixed.js
```

쓰기 시나리오는 합성 계정별 기본 배송지, 충분한 전용 상품 재고,
`LOAD_TEST_ENABLE_WRITES=true`를 추가로 요구한다.

## 한계

- 로컬 Docker Desktop 결과이며 네트워크가 분리된 실제 배포 환경과 다르다.
- 상품 2개, 리뷰 20개의 작은 데이터셋이라 검색·페이지네이션 병목을 대표하지 않는다.
- 주문 시나리오는 결제 승인과 외부 PG 네트워크 지연을 포함하지 않는다.
- 부하 생성기와 서비스가 같은 Docker Desktop 자원을 공유한다.
- 결과는 절대 성능 보증이 아니라 YMALL-77 개선 전후 비교의 기준선으로 사용한다.
