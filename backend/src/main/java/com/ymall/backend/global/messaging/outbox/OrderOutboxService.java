package com.ymall.backend.global.messaging.outbox;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.global.messaging.OrderEventType;

@Service
@RequiredArgsConstructor
public class OrderOutboxService {

    private final OrderOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Kafka로 직접 전송하지 않고 호출자의 트랜잭션에 이벤트를 저장한다.
     *
     * <p>Aggregate를 변경하는 호출자는 도메인 변경과 이벤트가 함께 커밋되거나 함께 롤백되도록
     * 같은 트랜잭션 안에서 이 메서드를 호출해야 한다. 사용할 수 없는 이벤트가 저장되지 않도록
     * 저장 전에 Payload 직렬화를 수행하고, 직렬화 실패 시 전체 트랜잭션을 실패시킨다.</p>
     */
    public OrderOutboxEvent save(
        OrderEventType eventType,
        Long orderId,
        Long memberId,
        Map<String, Object> payload
    ) {
        OrderEventEnvelope event = OrderEventEnvelope.create(
            eventType,
            orderId,
            memberId,
            payload
        );
        try {
            String serializedPayload = objectMapper.writeValueAsString(event.payload());
            return outboxEventRepository.save(new OrderOutboxEvent(event, serializedPayload));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize outbox payload.", exception);
        }
    }
}
