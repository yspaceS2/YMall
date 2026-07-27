package com.ymall.backend.settlement.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.seller.entity.SellerProfile;

@Getter
@Entity
@Table(name = "settlement_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false, updatable = false)
    private SellerProfile sellerProfile;

    @Column(name = "period_start", nullable = false, updatable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false, updatable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementRequestStatus status;

    @Column(name = "gross_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "fee_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "settlement_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal settlementAmount;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_member_id")
    private Member reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "mock_payment_reference", length = 80)
    private String mockPaymentReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SettlementRequest(
        SellerProfile sellerProfile,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal settlementAmount
    ) {
        this.sellerProfile = sellerProfile;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.status = SettlementRequestStatus.REQUESTED;
        updateAmounts(grossAmount, feeAmount, settlementAmount);
    }

    public SettlementRequestStatus resubmit(
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal settlementAmount
    ) {
        requireStatus(SettlementRequestStatus.REJECTED);
        SettlementRequestStatus previous = status;
        status = SettlementRequestStatus.REQUESTED;
        rejectionReason = null;
        reviewedBy = null;
        reviewedAt = null;
        updateAmounts(grossAmount, feeAmount, settlementAmount);
        return previous;
    }

    public SettlementRequestStatus approve(Member admin) {
        requireStatus(SettlementRequestStatus.REQUESTED);
        SettlementRequestStatus previous = status;
        status = SettlementRequestStatus.APPROVED;
        reviewedBy = admin;
        reviewedAt = Instant.now();
        rejectionReason = null;
        return previous;
    }

    public SettlementRequestStatus reject(Member admin, String reason) {
        requireStatus(SettlementRequestStatus.REQUESTED);
        SettlementRequestStatus previous = status;
        status = SettlementRequestStatus.REJECTED;
        reviewedBy = admin;
        reviewedAt = Instant.now();
        rejectionReason = reason;
        return previous;
    }

    public SettlementRequestStatus markPaid(Member admin, String paymentReference) {
        requireStatus(SettlementRequestStatus.APPROVED);
        SettlementRequestStatus previous = status;
        status = SettlementRequestStatus.PAID;
        reviewedBy = admin;
        paidAt = Instant.now();
        mockPaymentReference = paymentReference;
        return previous;
    }

    private void updateAmounts(
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal settlementAmount
    ) {
        this.grossAmount = grossAmount;
        this.feeAmount = feeAmount;
        this.settlementAmount = settlementAmount;
    }

    private void requireStatus(SettlementRequestStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Invalid settlement request status transition.");
        }
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
