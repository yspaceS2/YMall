package com.ymall.backend.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;

@Getter
@Entity
@Table(
    name = "payments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_payments_order_idempotency_key",
        columnNames = {"order_id", "idempotency_key"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Column(name = "payment_order_id", length = 64)
    private String paymentOrderId;

    @Column(name = "requested_amount", precision = 38, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "approved_amount", precision = 38, scale = 2)
    private BigDecimal approvedAmount;

    @Column(length = 50)
    private String method;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentResult result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Column(length = 255)
    private String failureMessage;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_status", length = 30)
    private PaymentGatewayStatus providerStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;

    public Payment(
        Order order,
        String idempotencyKey,
        PaymentResult result,
        String failureMessage
    ) {
        this.order = order;
        this.idempotencyKey = idempotencyKey;
        this.result = result;
        this.orderStatus = order.getStatus();
        this.failureMessage = failureMessage;
    }

    private Payment(
        Order order,
        String idempotencyKey,
        String paymentKey,
        String paymentOrderId,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        String method,
        OffsetDateTime approvedAt,
        PaymentResult result,
        String failureCode,
        String failureMessage
    ) {
        this.order = order;
        this.idempotencyKey = idempotencyKey;
        this.paymentKey = paymentKey;
        this.paymentOrderId = paymentOrderId;
        this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount;
        this.method = method;
        this.approvedAt = approvedAt;
        this.result = result;
        this.orderStatus = order.getStatus();
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.providerStatus = result == PaymentResult.SUCCESS
            ? PaymentGatewayStatus.DONE
            : null;
    }

    public static Payment success(
        Order order,
        String idempotencyKey,
        String paymentKey,
        String paymentOrderId,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        String method,
        OffsetDateTime approvedAt
    ) {
        return new Payment(
            order,
            idempotencyKey,
            paymentKey,
            paymentOrderId,
            requestedAmount,
            approvedAmount,
            method,
            approvedAt,
            PaymentResult.SUCCESS,
            null,
            null
        );
    }

    public static Payment failure(
        Order order,
        String idempotencyKey,
        String paymentKey,
        String paymentOrderId,
        BigDecimal requestedAmount,
        String failureCode,
        String failureMessage
    ) {
        return new Payment(
            order,
            idempotencyKey,
            paymentKey,
            paymentOrderId,
            requestedAmount,
            null,
            null,
            null,
            PaymentResult.FAILURE,
            failureCode,
            failureMessage
        );
    }

    public void synchronizeSuccess(PaymentGatewayResult gatewayResult) {
        this.paymentKey = gatewayResult.paymentKey();
        this.paymentOrderId = gatewayResult.orderId();
        this.requestedAmount = gatewayResult.totalAmount();
        this.approvedAmount = gatewayResult.totalAmount();
        this.method = gatewayResult.method();
        this.approvedAt = gatewayResult.approvedAt();
        this.result = PaymentResult.SUCCESS;
        this.orderStatus = order.getStatus();
        this.failureCode = null;
        this.failureMessage = null;
        this.providerStatus = gatewayResult.status();
    }

    public void synchronizeFailure(PaymentGatewayResult gatewayResult) {
        this.paymentKey = gatewayResult.paymentKey();
        this.paymentOrderId = gatewayResult.orderId();
        this.requestedAmount = gatewayResult.totalAmount();
        this.approvedAmount = null;
        this.method = gatewayResult.method();
        this.approvedAt = gatewayResult.approvedAt();
        this.result = PaymentResult.FAILURE;
        this.orderStatus = order.getStatus();
        this.failureCode = gatewayResult.status().name();
        this.failureMessage = "Payment status was synchronized by a verified webhook.";
        this.providerStatus = gatewayResult.status();
    }

    public void synchronizeProviderStatus(PaymentGatewayStatus providerStatus) {
        this.providerStatus = providerStatus;
        this.orderStatus = order.getStatus();
    }

    @PrePersist
    protected void onCreate() {
        this.processedAt = LocalDateTime.now();
    }
}
