package com.ymall.backend.payment.refund.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;

@Getter
@Entity
@Table(
    name = "payment_refunds",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_payment_refunds_order_idempotency_key",
        columnNames = {"order_id", "idempotency_key"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentRefundType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentRefundStatus status;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(name = "requested_by_member_id", nullable = false)
    private Long requestedByMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by_role", nullable = false, length = 30)
    private MemberRole requestedByRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_status", length = 30)
    private PaymentGatewayStatus providerStatus;

    @Column(name = "provider_balance_amount", precision = 38, scale = 2)
    private BigDecimal providerBalanceAmount;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentRefundItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public PaymentRefund(
        Payment payment,
        Order order,
        String idempotencyKey,
        PaymentRefundType type,
        String reason,
        Long requestedByMemberId,
        MemberRole requestedByRole
    ) {
        this.payment = payment;
        this.order = order;
        this.idempotencyKey = idempotencyKey;
        this.type = type;
        this.status = PaymentRefundStatus.PENDING;
        this.amount = BigDecimal.ZERO.setScale(2);
        this.reason = reason;
        this.requestedByMemberId = requestedByMemberId;
        this.requestedByRole = requestedByRole;
    }

    public void addItem(PaymentRefundItem item) {
        items.add(item);
        item.assignRefund(this);
        amount = amount.add(item.getAmount());
    }

    public void succeed(PaymentGatewayStatus providerStatus, BigDecimal providerBalanceAmount) {
        this.status = PaymentRefundStatus.SUCCEEDED;
        this.providerStatus = providerStatus;
        this.providerBalanceAmount = providerBalanceAmount;
        this.failureCode = null;
        this.failureMessage = null;
        this.processedAt = LocalDateTime.now();
    }

    public void fail(String failureCode, String failureMessage, boolean outcomeUnknown) {
        if (status != PaymentRefundStatus.PENDING) {
            return;
        }
        this.status = outcomeUnknown
            ? PaymentRefundStatus.UNKNOWN
            : PaymentRefundStatus.FAILED;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.processedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
