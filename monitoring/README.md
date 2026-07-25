# YMall 로컬 성능 모니터링

YMALL-75에서 구성한 Prometheus·Grafana 기반 로컬 모니터링 환경입니다. 부하 테스트를
실행하는 동안 Backend, PostgreSQL, Redis, Kafka와 Docker 컨테이너 지표를 같은 시간축으로
확인하는 용도입니다.

모니터링 컨테이너는 기본 `docker compose up`에는 포함되지 않습니다. 필요할 때만
`monitoring` 프로필로 실행해 평상시 메모리 사용량을 늘리지 않습니다.

## 구성

| 대상 | 수집 방식 | 주요 지표 |
| --- | --- | --- |
| Spring Backend | Actuator + Micrometer Prometheus | HTTP 처리량·지연·오류율, JVM, GC, HikariCP |
| PostgreSQL | postgres_exporter + pg_stat_statements | 연결, 잠금, 트랜잭션, 쿼리 실행 시간 |
| Redis | redis_exporter | hit/miss, 명령 지연, 메모리, 연결 |
| Kafka | kafka_exporter + 애플리케이션 카운터 | Topic offset, Consumer Lag, 재시도, DLT, Outbox |
| Docker | cAdvisor | 컨테이너별 CPU·메모리 |

PostgreSQL의 `pg_stat_statements` 수집에는 query ID와 실행 통계만 사용합니다. SQL 원문은
Prometheus에 노출하지 않습니다.

## 실행

프로젝트 루트에서 애플리케이션과 모니터링 환경을 함께 실행합니다.

```powershell
docker compose --profile monitoring up -d --build
```

접속 주소:

- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9091`
- Grafana 대시보드: `YMall / YMall Performance Overview`

두 포트는 `127.0.0.1`에만 바인딩됩니다. Grafana는 로컬 조회 전용 anonymous viewer로
구성되어 로그인 화면과 설정 변경 기능을 비활성화했습니다. 외부 서버에 그대로 공개하지
않습니다.

## 부하 테스트와 함께 실행

모니터링을 먼저 실행한 다음 별도 터미널에서 YMALL-74 시나리오를 실행합니다.

```powershell
$env:LOAD_TEST_SCENARIO = "read"
docker compose --profile monitoring --profile load-test run --rm k6
```

Grafana 시간 범위를 테스트 시작 전후로 맞추면 애플리케이션과 인프라 지표를 같은
시간축에서 비교할 수 있습니다.

## 정상 수집 확인

1. Prometheus `Status > Target health`에서 아래 다섯 job이 `UP`인지 확인합니다.
   - `ymall-backend`
   - `ymall-postgresql`
   - `ymall-redis`
   - `ymall-kafka`
   - `ymall-containers`
2. Grafana의 `YMall Performance Overview`를 열어 패널에 값이 들어오는지 확인합니다.
3. 부하 테스트 중 Backend throughput과 request latency가 변하는지 확인합니다.

컨테이너 상태는 다음 명령으로도 확인할 수 있습니다.

```powershell
docker compose --profile monitoring ps
```

## 주요 지표 해석

### Backend와 JVM

- `http_server_requests_seconds_count`: 요청 처리량 계산에 사용합니다.
- `http_server_requests_seconds_bucket`: p95·p99 HTTP 지연을 계산합니다.
- `jvm_memory_used_bytes`: heap 사용량 증가와 테스트 후 회수 여부를 봅니다.
- `jvm_gc_pause_seconds_*`: GC 정지 시간이 응답 지연과 겹치는지 확인합니다.
- `hikaricp_connections_active/pending/max`: DB 연결 풀 고갈 여부를 확인합니다.

### PostgreSQL

- `pg_stat_database_numbackends`: 데이터베이스 연결 수입니다.
- `pg_locks_count`: 잠금 모드별 대기 가능성을 확인합니다.
- `pg_stat_database_blks_hit/read`: shared buffer hit 비율 계산에 사용합니다.
- `pg_stat_statements_*`: query ID별 호출 수와 평균·전체 실행 시간입니다.

`pg_stat_statements`는 PostgreSQL 시작 옵션과 extension이 모두 필요합니다.
`postgres-monitoring-init` 컨테이너가 extension을 멱등하게 생성하고 종료합니다.

### Redis

- `redis_keyspace_hits_total`, `redis_keyspace_misses_total`: 캐시 적중률 계산에 사용합니다.
- `redis_memory_used_bytes`: 캐시 데이터와 오버헤드 메모리입니다.
- `redis_connected_clients`: 연결 수 증가를 확인합니다.
- `redis_commands_duration_seconds_total`: 명령 처리 지연을 확인합니다.

### Kafka

- `kafka_consumergroup_lag`: Consumer가 아직 처리하지 못한 메시지 수입니다.
- `kafka_topic_partition_current_offset`: Topic·Partition별 누적 offset입니다.
- `ymall_kafka_consumer_retries_total`: 애플리케이션 Consumer 재시도 횟수입니다.
- `ymall_kafka_consumer_dead_letters_total`: DLT 전송 횟수입니다.
- `ymall_kafka_outbox_published_total`: Outbox 발행 성공 횟수입니다.
- `ymall_kafka_outbox_publish_failures_total`: 일시·영구 Outbox 발행 실패 횟수입니다.

Topic 이름만 label로 사용하며 event ID, 주문 ID, 회원 ID처럼 값의 종류가 계속 늘어나는
정보는 metric label로 사용하지 않습니다.
재시도·DLT·Outbox 사용자 지표는 해당 이벤트가 한 번 이상 발생한 뒤 시계열이 생성되므로,
이벤트가 없었던 구간에는 Grafana가 `No data`로 표시할 수 있습니다.

## 로그가 결과에 미치는 영향

- 성능 측정용 `prod` profile에서는 Hibernate SQL과 bind parameter 로그를 출력하지
  않습니다.
- Kafka 재시도와 일시적인 Outbox 실패는 예외 stack trace 대신 오류 종류만 기록합니다.
- 영구 Outbox 실패는 장애 분석을 위해 error stack trace를 유지합니다.
- Prometheus scrape interval은 5초이며 로컬 데이터는 최대 24시간 또는 1GB까지만
  보존합니다.

## 종료

모니터링 컨테이너만 중지하려면 다음 명령을 사용합니다.

```powershell
docker compose --profile monitoring stop grafana prometheus cadvisor kafka-exporter redis-exporter postgres-exporter
```

`postgres-monitoring-init`는 extension을 생성한 뒤 정상 종료되는 일회성 컨테이너입니다.
Prometheus와 Grafana named volume은 다음 실행을 위해 유지됩니다.

## Docker Desktop 참고

cAdvisor는 Docker 엔진의 Linux VM 정보를 읽기 위해 privileged 권한과 read-only 시스템
mount를 사용합니다. 이는 로컬 `monitoring` 프로필에서만 활성화됩니다. Docker Desktop
버전에 따라 `ymall-containers` target이 내려가면 먼저 cAdvisor 로그와 mount 지원 여부를
확인하고, 해당 구간의 보조 자료로 `docker stats`를 사용합니다.
