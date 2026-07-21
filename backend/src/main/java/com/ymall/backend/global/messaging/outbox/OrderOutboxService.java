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
