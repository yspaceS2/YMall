package com.ymall.backend.payment.entity;

import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentResult result;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Column(length = 255)
    private String failureMessage;

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

    @PrePersist
    protected void onCreate() {
        this.processedAt = LocalDateTime.now();
    }
}
