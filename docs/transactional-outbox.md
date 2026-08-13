# 주문 이벤트 Transactional Outbox

## 목적

주문 상태 변경과 Kafka 이벤트 발행 요청을 하나의 PostgreSQL 트랜잭션에 저장한다.
Kafka가 일시적으로 중단되더라도 주문 트랜잭션은 Outbox 이벤트를 남기며, Relay가 이후
재전송한다.

## 처리 흐름

1. 주문 생성, 결제, 취소 또는 배송 상태 변경 트랜잭션이 실행된다.
2. 같은 트랜잭션에서 `order_outbox_events`에 `PENDING` 이벤트를 저장한다.
3. Relay가 발행 가능한 이벤트를 오래된 순서로 잠금 조회한다.
4. Kafka 발행 성공 시 이벤트를 `PUBLISHED`로 변경한다.
5. 실패 시 `attempt_count`, `last_error`, `next_attempt_at`을 기록한다.
6. 최대 시도 횟수에 도달하면 `FAILED`로 변경해 자동 발행을 중단한다.
7. `PUBLISHED` 이벤트는 보존 기간이 지나면 정리한다.

## 전달 보장

Outbox Relay는 **at-least-once** 방식으로 동작한다. Kafka 발행은 성공했지만 DB의
`PUBLISHED` 변경 커밋이 실패하면 같은 `eventId`가 다시 전달될 수 있다. Consumer는
`PUBLISHED` 변경 커밋이 실패하면 같은 `eventId`가 다시 전달될 수 있다. 현재 주문 알림 Consumer와 정산 처리기는 이벤트 ID를 처리 이력과 비교해 이미 처리한 이벤트의 부수 효과가 반복되지 않도록 구성한다.

리뷰 요약 갱신도 Kafka를 사용하지만 주문 Outbox와는 목적이 다르다. 리뷰 변경 요청은 저장 이후 요약 갱신 이벤트를 발행하며, 동일 상품의 동시 추론은 Redis 잠금으로 제한한다. 주문 상태와 함께 원자적으로 보장해야 하는 이벤트는 이 문서의 Outbox 흐름을 사용한다.

## 상태

| 상태 | 의미 |
| --- | --- |
| `PENDING` | 최초 발행 또는 재시도 대기 |
| `PUBLISHED` | Kafka 발행 완료 |
| `FAILED` | 최대 시도 횟수 초과로 자동 발행 중단 |

## 환경변수

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `KAFKA_OUTBOX_ENABLED` | `true` | Outbox Relay 스케줄러 활성화 |
| `KAFKA_OUTBOX_BATCH_SIZE` | `100` | 한 번에 처리할 이벤트 수 |
| `KAFKA_OUTBOX_MAX_ATTEMPTS` | `10` | 이벤트별 최대 실패 횟수 |
| `KAFKA_OUTBOX_RETRY_DELAY` | `5s` | 실패 후 재시도 대기 시간 |
| `KAFKA_OUTBOX_SEND_TIMEOUT` | `10s` | Kafka 전송 결과 대기 시간 |
| `KAFKA_OUTBOX_POLL_INTERVAL` | `1s` | 미발행 이벤트 조회 주기 |
| `KAFKA_OUTBOX_CLEANUP_INTERVAL` | `1h` | 완료 이벤트 정리 주기 |
| `KAFKA_OUTBOX_PUBLISHED_RETENTION` | `7d` | 완료 이벤트 보존 기간 |

## 검증

```shell
cd backend
./gradlew postgresTest --tests "com.ymall.backend.integration.kafka.OrderOutboxIntegrationTest"
./gradlew test --tests "com.ymall.backend.integration.order.OrderApiIntegrationTest"
```

테스트는 주문과 Outbox의 동시 롤백, Kafka 장애 후 재발행, 최대 시도 횟수 이후 발행
중단을 검증한다.
