# Kafka 개발 환경과 주문 이벤트 규격

## 로컬 실행

프로젝트 루트에서 Redis와 Kafka를 실행한다.

```shell
docker compose up -d redis kafka
docker compose ps
```

Kafka는 KRaft 단일 브로커로 실행되며 호스트 애플리케이션은
`localhost:9092`로 연결한다. 자동 토픽 생성을 비활성화하고 Spring의 `NewTopic`
설정으로 필요한 토픽을 생성한다.

## 환경변수

| 환경변수 | 기본값 | 설명 |
| --- | --- | --- |
| `KAFKA_ENABLED` | `true` | Kafka 토픽 및 Producer 활성화 여부 |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka 브로커 주소 |
| `KAFKA_CONSUMER_GROUP` | `ymall-local` | 기본 Consumer Group |
| `KAFKA_ORDER_EVENTS_TOPIC` | `ymall.order.events.v1` | 주문 이벤트 토픽 |
| `KAFKA_ORDER_EVENTS_PARTITIONS` | `3` | 파티션 수 |
| `KAFKA_ORDER_EVENTS_REPLICATION_FACTOR` | `1` | 로컬 복제 계수 |
| `KAFKA_ORDER_EVENTS_RETENTION` | `7d` | 메시지 보존 기간 |

## 토픽 규칙

- 형식: `ymall.{domain}.{purpose}.v{schemaVersion}`
- 주문 이벤트: `ymall.order.events.v1`
- 메시지 Key: `orderId`
- 같은 주문의 이벤트는 동일 파티션으로 전달되어 파티션 내부 순서를 유지한다.
- 로컬 환경은 브로커가 하나이므로 복제 계수는 1이다. 운영 다중 브로커 환경에서는 3을 권장한다.

## 공통 이벤트 Envelope

```json
{
  "eventId": "d154e893-a46b-46e6-a1ae-14a8ec2317eb",
  "eventType": "ORDER_CREATED",
  "occurredAt": "2026-07-21T00:00:00Z",
  "orderId": 101,
  "memberId": 20,
  "payload": {
    "totalAmount": 39000
  },
  "version": 1
}
```

- `eventId`: Consumer 중복 처리 방지를 위한 이벤트 고유 ID
- `eventType`: 주문 생명주기 이벤트 종류
- `occurredAt`: 이벤트 발생 UTC 시각
- `orderId`: 파티션 Key와 주문 식별자
- `memberId`: 이벤트 대상 회원
- `payload`: 이벤트 종류별 확장 데이터
- `version`: Envelope 스키마 버전

기존 필드의 의미나 타입을 변경하지 않고 `payload`에 선택 필드를 추가하는 변경은 같은
버전을 유지한다. 필수 필드 변경이나 호환되지 않는 타입 변경은 새 토픽 버전을 사용한다.

## 검증

```shell
cd backend
./gradlew test --tests "com.ymall.backend.global.messaging.OrderEventEnvelopeSerializationTest"
./gradlew test --tests "com.ymall.backend.integration.kafka.KafkaOrderEventIntegrationTest"
```

첫 번째 테스트는 JSON 규격 호환성을, 두 번째 테스트는 Embedded Kafka에서 실제
Producer·Consumer 송수신과 3개 파티션 토픽 생성을 검증한다.
