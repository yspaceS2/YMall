package com.ymall.backend.global.messaging;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record OrderEventEnvelope(
    UUID eventId,
    OrderEventType eventType,
    Instant occurredAt,
    Long orderId,
    Long memberId,
    Map<String, Object> payload,
    int version
) {

    public static final int CURRENT_VERSION = 1;

    public OrderEventEnvelope {
        if (eventId == null || eventType == null || occurredAt == null
            || orderId == null || memberId == null || payload == null || version < 1) {
            throw new IllegalArgumentException("Order event fields must not be null or invalid.");
        }
        payload = Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    public static OrderEventEnvelope create(
        OrderEventType eventType,
        Long orderId,
        Long memberId,
        Map<String, Object> payload
    ) {
        return new OrderEventEnvelope(
            UUID.randomUUID(),
            eventType,
            Instant.now(),
            orderId,
            memberId,
            payload,
            CURRENT_VERSION
        );
    }
}
