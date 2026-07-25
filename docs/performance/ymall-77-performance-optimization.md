# YMALL-77 주문 생성 성능 개선 결과

## 결론

주문 생성 과정에서 `orders.payment_order_id`에 동일한 고유 인덱스가 두 개 생성되는 문제를
확인하고, 스키마 소유권을 Flyway로 일원화해 하나만 유지하도록 수정했다.

동일한 로컬 Docker 환경과 k6 조건에서 주문 생성 p95는 29.74ms에서 20.86ms로 29.9%
감소했다. PostgreSQL에 주문 1,000건을 삽입하고 롤백하는 마이크로 벤치마크의 중앙값도
40.01ms에서 35.23ms로 12.0% 감소했다.

API 비교는 전후 각각 주문 16건의 작은 표본으로 실행했기 때문에, 29.9%를 운영 환경의
보장 수치로 사용하지 않는다. 반복 횟수가 더 많은 DB 마이크로 벤치마크와 함께 보면
중복 인덱스 제거가 쓰기 비용을 줄이는 방향은 일관되게 확인된다.

## 병목 분석

주문 생성 트랜잭션은 다음 작업을 한 번에 수행한다.

1. 회원과 장바구니 항목 잠금
2. 상품 잠금 및 재고 차감
3. 주문과 주문 항목 저장
4. 장바구니 삭제
5. Outbox 이벤트 저장

부하 측정 직전에 `pg_stat_statements`를 초기화하고 주문 관련 SQL만 분리해서 확인했다.
각 SQL의 평균 실행 시간은 짧았으며, 단일 슬로우 쿼리보다 여러 쓰기와 잠금이 누적되는
트랜잭션 특성이 응답 시간에 더 큰 영향을 줬다.

| SQL | 호출 수 | 전체 실행 시간 | 평균 실행 시간 |
| --- | ---: | ---: | ---: |
| 주문 저장 | 16 | 3.602ms | 0.225ms |
| 주문 항목 저장 | 16 | 3.812ms | 0.238ms |
| 장바구니 항목 저장 | 14 | 3.051ms | 0.218ms |
| 회원 잠금 | 32 | 1.999ms | 0.062ms |
| Outbox 저장 | 16 | 1.706ms | 0.107ms |
| 상품 재고 갱신 | 16 | 1.348ms | 0.084ms |
| 장바구니 삭제 | 16 | 0.450ms | 0.028ms |

`orders` 테이블을 조사한 결과 `payment_order_id` 한 컬럼에 아래 두 고유 인덱스가
중복으로 존재했다.

- Hibernate가 엔티티의 `unique = true`를 보고 만든 제약조건 기반 인덱스
- Flyway V5가 명시적으로 만든 `uk_orders_payment_order_id`

두 인덱스는 같은 유일성을 검사하므로 조회 기능은 늘지 않지만, 주문을 저장할 때마다
두 B-tree를 모두 갱신해야 한다.

## 변경 내용

- `Order.paymentOrderId`에서 Hibernate 스키마 생성용 `unique = true`를 제거했다.
- Flyway V9에서 표준 고유 인덱스 `uk_orders_payment_order_id`를 먼저 보장한다.
- 같은 단일 컬럼에 걸린 이름이 다른 고유 제약조건만 제거한다.
- 주문번호의 유일성은 기존과 동일하게 PostgreSQL에서 보장한다.
- 회원별 멱등성 키 제약조건은 변경하지 않았다.

기존에 적용된 V5 마이그레이션은 수정하지 않았다. 이미 실행된 마이그레이션의 체크섬을
바꾸지 않고 V9 보정 마이그레이션으로 기존 DB와 신규 DB를 같은 상태로 맞춘다.

## 측정 조건

| 항목 | 값 |
| --- | --- |
| 실행 환경 | 로컬 Docker Desktop |
| Backend | Spring Boot, Java 17, prod profile |
| 데이터베이스 | PostgreSQL 16 |
| 부하 도구 | k6 2.1.0 |
| 상품 | 승인된 상품 ID 2 |
| VU | 2 |
| 단계 | 10초 증가, 30초 유지, 10초 감소 |
| 요청 간 대기 | 1초 |
| 토큰 갱신 | 20회마다 |
| 주문 생성 | 5회마다 |
| 전후 HTTP 요청 | 각각 200회 |
| 전후 반복 | 각각 80회 |
| 전후 주문 생성 | 각각 16건 |

측정용 계정은 실행 중에만 임의 생성했으며, 비밀번호와 토큰을 결과 파일이나 문서에
기록하지 않았다. 전후 측정은 동일한 시나리오와 옵션을 사용했고, 준비 데이터 생성 후
`pg_stat_statements`를 초기화했다.

## API 전후 비교

| 지표 | 적용 전 | 적용 후 | 변화 |
| --- | ---: | ---: | ---: |
| 주문 생성 평균 | 17.09ms | 14.16ms | -17.1% |
| 주문 생성 p50 | 14.60ms | 14.52ms | -0.6% |
| 주문 생성 p95 | 29.74ms | 20.86ms | -29.9% |
| 주문 생성 p99 | 35.81ms | 21.30ms | -40.5% |
| 주문 생성 최대 | 37.33ms | 21.41ms | -42.6% |
| 전체 HTTP p95 | 17.19ms | 14.54ms | -15.4% |
| 전체 HTTP p99 | 37.65ms | 21.81ms | -42.1% |
| HTTP 오류율 | 0% | 0% | 동일 |
| check 성공률 | 100% | 100% | 동일 |

원본 결과:

- [적용 전 k6 JSON](./ymall-77-before.json)
- [적용 후 k6 JSON](./ymall-77-after.json)

## DB 마이크로 벤치마크

API 외부 요인의 영향을 줄이기 위해 같은 DB에서 주문 1,000건 삽입을 실행한 뒤 전체
트랜잭션을 롤백했다. 각 조건을 5회 실행했다.

| 조건 | 1회 | 2회 | 3회 | 4회 | 5회 | 중앙값 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 중복 인덱스 | 37.34ms | 40.01ms | 89.15ms | 45.77ms | 38.63ms | 40.01ms |
| 단일 인덱스 | 34.37ms | 39.18ms | 65.97ms | 35.23ms | 33.79ms | 35.23ms |

중앙값은 12.0% 감소했다. 양쪽 조건에 큰 이상치가 한 번씩 있었기 때문에 평균보다
중앙값을 대표 수치로 사용했다.

## 재현 방법

루트 `.env`에는 실제 운영 계정이 아닌 부하 테스트 전용 계정의
`LOAD_TEST_USER_EMAILS`, `LOAD_TEST_USER_PASSWORDS`만 설정한다. 계정 수는 VU 이상이어야
하며 각 계정에는 기본 배송지가 필요하다. 비밀번호와 토큰은 명령, 결과 파일, 문서에
기록하지 않는다.

먼저 YMall 컨테이너를 실행하고 아래 환경 변수를 동일하게 설정한다.

```powershell
docker compose up -d

$env:LOAD_TEST_SCENARIO = "mixed"
$env:LOAD_TEST_ENABLE_WRITES = "true"
$env:LOAD_TEST_PRODUCT_ID = "2"
$env:LOAD_TEST_TARGET_VUS = "2"
$env:LOAD_TEST_RAMP_UP = "10s"
$env:LOAD_TEST_HOLD = "30s"
$env:LOAD_TEST_RAMP_DOWN = "10s"
$env:LOAD_TEST_THINK_TIME = "1"
$env:LOAD_TEST_REFRESH_EVERY = "20"
$env:LOAD_TEST_WRITE_EVERY = "5"
```

개선 전 조건은 애플리케이션의 스키마 정의를 바꾸지 않고, 로컬 측정 DB에 같은 컬럼의
두 번째 고유 인덱스를 측정 중에만 추가해 재현한다. 계정과 상품 준비가 끝난 뒤 통계를
초기화하고 측정한다.

```powershell
"CREATE UNIQUE INDEX ymall77_benchmark_duplicate_payment_order_id ON orders (payment_order_id);" |
  docker compose exec -T postgres sh -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'

"SELECT pg_stat_statements_reset();" |
  docker compose exec -T postgres sh -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'

docker compose --profile load-test run --rm k6 run `
  --summary-export=/results/ymall-77-before.json `
  /scripts/scenarios/mixed.js
```

개선 후 조건은 임시 중복 인덱스를 제거한 직후 통계를 다시 초기화하고, 다른 설정을
바꾸지 않은 채 같은 시나리오를 실행한다. 앞선 실행이 실패해도 임시 인덱스는 반드시
제거한다.

```powershell
"DROP INDEX IF EXISTS ymall77_benchmark_duplicate_payment_order_id;" |
  docker compose exec -T postgres sh -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'

"SELECT pg_stat_statements_reset();" |
  docker compose exec -T postgres sh -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'

docker compose --profile load-test run --rm k6 run `
  --summary-export=/results/ymall-77-after.json `
  /scripts/scenarios/mixed.js
```

DB 마이크로 벤치마크는 아래 스크립트를 5회 실행한다. 한 번의 실행 안에서 중복 인덱스
조건과 단일 인덱스 조건을 차례로 측정하며, 두 삽입은 모두 롤백된다. `EXPLAIN ANALYZE`
출력의 `Execution Time` 중앙값을 비교한다. 주문 ID 시퀀스 값은 롤백되지 않으므로
운영 DB가 아닌 로컬 측정 DB에서만 실행한다.

```powershell
1..5 | ForEach-Object {
  Get-Content load-test/sql/ymall-77-order-index-benchmark.sql |
    docker compose exec -T postgres sh -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
}
```

## 정합성 검증

- Flyway V9가 PostgreSQL에 정상 적용됐다.
- `payment_order_id`의 고유 인덱스는 `uk_orders_payment_order_id` 하나만 남았다.
- 주문번호 유일성과 회원별 멱등성 제약은 유지된다.
- 전후 k6 실행 모두 200개 check가 성공했고 HTTP 오류는 없었다.
- 주문 서비스, 주문 API, 주문 동시성 테스트가 통과했다.
- 기존 YMall Redis를 사용한 컨테이너 환경에서 전체 백엔드 테스트가 통과했다.

## 서버 규모 추정

YMALL-76에서 낮은 부하 동안 Backend working set은 약 575MiB였고 HikariCP 대기와 Kafka
Consumer Lag은 0이었다. 이 값과 이번 결과를 기준으로 한 보수적인 시작점은 다음과 같다.

| 배치 방식 | 시작 사양 | 용도 |
| --- | --- | --- |
| 애플리케이션, PostgreSQL, Redis, Kafka 통합 | 4 vCPU, 8GB RAM | 포트폴리오·소규모 검증 |
| AI CPU 추론까지 같은 서버에 통합 | 8 vCPU, 16GB RAM 이상 | 저빈도 요약 요청 |

이는 용량 보장이 아니라 로컬 측정에 기반한 시작점이다. 운영 배포 전에는 운영과 유사한
데이터 양, 네트워크, 동시 사용자 수로 단계별 부하 테스트를 실행하고 CPU, 메모리,
PostgreSQL I/O, HikariCP pending, Kafka lag, AI 추론 큐 대기를 함께 관찰해야 한다.

## 한계와 후속 작업

- 로컬 Docker Desktop의 공유 자원과 백그라운드 작업 영향을 받는다.
- API 전후 비교가 각각 한 번이며 주문 표본이 16건으로 작다.
- 상품과 계정 데이터 규모가 운영 환경보다 작다.
- 결제 승인과 외부 PG 네트워크 지연은 포함하지 않았다.
- 대용량 데이터에서의 인덱스 생성은 잠금과 디스크 사용량을 별도로 검토해야 한다.
- 다음 단계에서는 10, 50, 100 VU로 부하를 높이고 각 조건을 여러 번 반복해 처리량이
  선형으로 증가하지 않는 지점과 자원 포화 원인을 찾는다.
