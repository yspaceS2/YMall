package com.ymall.backend.payment.webhook.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.payment.gateway.PaymentGatewayStatus;

@Getter
@Entity
@Table(
    name = "payment_webhook_events",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_payment_webhook_events_transmission_id",
        columnNames = "transmission_id"
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transmission_id", nullable = false, updatable = false, length = 200)
    private String transmissionId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private String eventType;

    @Column(name = "payment_key", nullable = false, updatable = false, length = 200)
    private String paymentKey;

    @Column(name = "payment_order_id", nullable = false, updatable = false, length = 64)
    private String paymentOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_status", nullable = false, updatable = false, length = 30)
    private PaymentGatewayStatus requestedStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified_status", nullable = false, updatable = false, length = 30)
    private PaymentGatewayStatus verifiedStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private PaymentWebhookProcessingResult result;

    @Column(name = "event_created_at", nullable = false, updatable = false)
    private LocalDateTime eventCreatedAt;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    public PaymentWebhookEvent(
        String transmissionId,
        String eventType,
        String paymentKey,
        String paymentOrderId,
        PaymentGatewayStatus requestedStatus,
        PaymentGatewayStatus verifiedStatus,
        PaymentWebhookProcessingResult result,
        LocalDateTime eventCreatedAt
    ) {
        this.transmissionId = transmissionId;
        this.eventType = eventType;
        this.paymentKey = paymentKey;
        this.paymentOrderId = paymentOrderId;
        this.requestedStatus = requestedStatus;
        this.verifiedStatus = verifiedStatus;
        this.result = result;
        this.eventCreatedAt = eventCreatedAt;
        this.processedAt = LocalDateTime.now();
    }
}
