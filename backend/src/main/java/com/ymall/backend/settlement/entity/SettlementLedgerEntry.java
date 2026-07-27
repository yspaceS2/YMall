package com.ymall.backend.settlement.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.payment.refund.entity.PaymentRefund;
import com.ymall.backend.seller.entity.SellerProfile;

@Getter
@Entity
@Table(name = "settlement_ledger_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false, updatable = false)
    private SellerProfile sellerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, updatable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_refund_id", updatable = false)
    private PaymentRefund paymentRefund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_request_id")
    private SettlementRequest settlementRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20, updatable = false)
    private SettlementEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "gross_amount", nullable = false, precision = 38, scale = 2, updatable = false)
    private BigDecimal grossAmount;

    @Column(name = "fee_amount", nullable = false, precision = 38, scale = 2, updatable = false)
    private BigDecimal feeAmount;

    @Column(
        name = "settlement_amount",
        nullable = false,
        precision = 38,
        scale = 2,
        updatable = false
    )
    private BigDecimal settlementAmount;

    @Column(name = "source_event_id", nullable = false, updatable = false)
    private UUID sourceEventId;

    @Column(name = "source_key", nullable = false, unique = true, length = 120, updatable = false)
    private String sourceKey;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private SettlementLedgerEntry(
        SellerProfile sellerProfile,
        Order order,
        OrderItem orderItem,
        PaymentRefund paymentRefund,
        SettlementEntryType entryType,
        SettlementStatus status,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        UUID sourceEventId,
        String sourceKey,
        Instant occurredAt
    ) {
        this.sellerProfile = sellerProfile;
        this.order = order;
        this.orderItem = orderItem;
        this.paymentRefund = paymentRefund;
        this.entryType = entryType;
        this.status = status;
        this.grossAmount = grossAmount;
        this.feeAmount = feeAmount;
        this.settlementAmount = grossAmount.subtract(feeAmount);
        this.sourceEventId = sourceEventId;
        this.sourceKey = sourceKey;
        this.occurredAt = occurredAt;
    }

    public static SettlementLedgerEntry sale(
        SellerProfile sellerProfile,
        OrderItem orderItem,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        UUID sourceEventId,
        Instant occurredAt
    ) {
        return new SettlementLedgerEntry(
            sellerProfile,
            orderItem.getOrder(),
            orderItem,
            null,
            SettlementEntryType.SALE,
            SettlementStatus.PENDING,
            grossAmount,
            feeAmount,
            sourceEventId,
            "SALE:" + orderItem.getId(),
            occurredAt
        );
    }

    public static SettlementLedgerEntry refund(
        SellerProfile sellerProfile,
        PaymentRefund refund,
        OrderItem orderItem,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        UUID sourceEventId,
        Instant occurredAt
    ) {
        return new SettlementLedgerEntry(
            sellerProfile,
            orderItem.getOrder(),
            orderItem,
            refund,
            SettlementEntryType.REFUND,
            SettlementStatus.PENDING,
            grossAmount.negate(),
            feeAmount.negate(),
            sourceEventId,
            "REFUND:" + refund.getId() + ":" + orderItem.getId(),
            occurredAt
        );
    }

    public void makeAvailable() {
        if (status == SettlementStatus.PENDING) {
            status = SettlementStatus.AVAILABLE;
        }
    }

    public void requestSettlement(SettlementRequest request) {
        if (status != SettlementStatus.AVAILABLE) {
            throw new IllegalStateException("Only available ledger entries can be requested.");
        }
        settlementRequest = request;
        status = SettlementStatus.REQUESTED;
    }

    public void releaseSettlementRequest() {
        if (status != SettlementStatus.REQUESTED) {
            throw new IllegalStateException("Only requested ledger entries can be released.");
        }
        settlementRequest = null;
        status = SettlementStatus.AVAILABLE;
    }

    public void markPaid() {
        if (status != SettlementStatus.REQUESTED) {
            throw new IllegalStateException("Only requested ledger entries can be paid.");
        }
        status = SettlementStatus.PAID;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
