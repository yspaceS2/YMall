# Kafka Consumer 재시도와 DLT 운영

## 처리 정책

- 일시적인 런타임 오류는 `KAFKA_CONSUMER_MAX_RETRIES` 횟수만큼 재시도한다.
- 재시도 간격은 `KAFKA_CONSUMER_RETRY_DELAY`로 설정한다.
- `BusinessException`, `IllegalArgumentException`, 역직렬화 오류는 재시도하지 않는다.
- 재시도 소진 또는 비재시도 오류 이벤트는 `ymall.order.events.v1.DLT`에 저장한다.
- DLT 보존 기간은 기본 30일이며 `KAFKA_DLT_RETENTION`으로 변경할 수 있다.

## DLT 조회

토픽 상태를 확인한다.

```bash
docker exec ymall-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic ymall.order.events.v1.DLT
```

DLT 이벤트의 키, 값, 헤더를 확인한다.

```bash
docker exec ymall-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic ymall.order.events.v1.DLT \
  --from-beginning \
  --property print.key=true \
  --property print.value=true \
  --property print.headers=true
```

DLT 헤더에는 원본 토픽, 파티션, 오프셋과 예외 정보가 포함된다.

## 수동 재처리

1. DLT 헤더의 예외와 원본 이벤트 값을 확인한다.
2. 데이터 또는 설정 문제를 먼저 해결한다.
3. 원본 `orderId`를 키로 사용해 이벤트 JSON을 원본 토픽에 다시 발행한다.

```bash
docker exec -i ymall-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic ymall.order.events.v1 \
  --property parse.key=true \
  --property key.separator=:
```

입력 형식은 `orderId:eventJson`이다. 예를 들어 주문 ID가 10이면 다음과 같이 입력한다.

```text
10:{"eventId":"...","eventType":"ORDER_CREATED",...}
```

기존 `eventId`를 그대로 재발행해야 한다. 이미 정상 처리된 이벤트는
`processed_order_events.event_id`의 멱등성 검사로 다시 반영되지 않는다.
