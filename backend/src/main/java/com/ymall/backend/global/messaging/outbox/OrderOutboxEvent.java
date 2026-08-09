package com.ymall.backend.global.messaging.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.global.messaging.OrderEventType;

@Getter
@Entity
@Table(name = "order_outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderOutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private OrderEventType eventType;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, updatable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OrderOutboxEvent(OrderEventEnvelope event, String payload) {
        this.eventId = event.eventId();
        this.eventType = event.eventType();
        this.occurredAt = event.occurredAt();
        this.orderId = event.orderId();
        this.memberId = event.memberId();
        this.payload = payload;
        this.version = event.version();
        this.status = OutboxEventStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = event.occurredAt();
        this.createdAt = event.occurredAt();
    }

    @SuppressWarnings("unchecked")
    public OrderEventEnvelope toEnvelope(ObjectMapper objectMapper) {
        try {
            Map<String, Object> eventPayload = objectMapper.readValue(payload, Map.class);
            return new OrderEventEnvelope(
                eventId,
                eventType,
                occurredAt,
                orderId,
                memberId,
                eventPayload,
                version
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to deserialize outbox payload.", exception);
        }
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(String errorMessage, Instant nextAttemptAt, int maxAttempts) {
        this.attemptCount++;
        this.lastError = truncate(errorMessage);
        this.nextAttemptAt = nextAttemptAt;
        if (attemptCount >= maxAttempts) {
            this.status = OutboxEventStatus.FAILED;
        }
    }

    private String truncate(String errorMessage) {
        if (errorMessage == null) {
            return "Unknown Kafka publish failure";
        }
        return errorMessage.length() <= 1000 ? errorMessage : errorMessage.substring(0, 1000);
    }
}
