package com.ymall.backend.order.returnrequest.entity;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.payment.refund.entity.PaymentRefund;

@Getter
@Entity
@Table(name = "product_return_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnRequestStatus status;

    @Column(name = "seller_response", length = 500)
    private String sellerResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_refund_id")
    private PaymentRefund paymentRefund;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public ProductReturnRequest(
        OrderItem orderItem,
        Member member,
        int quantity,
        String reason,
        LocalDateTime requestedAt
    ) {
        this.orderItem = orderItem;
        this.member = member;
        this.quantity = quantity;
        this.reason = reason;
        this.status = ReturnRequestStatus.REQUESTED;
        this.requestedAt = requestedAt;
    }

    public void approve(
        PaymentRefund paymentRefund,
        String sellerResponse,
        LocalDateTime processedAt
    ) {
        validateRequested();
        this.status = ReturnRequestStatus.APPROVED;
        this.paymentRefund = paymentRefund;
        this.sellerResponse = sellerResponse;
        this.processedAt = processedAt;
    }

    public void reject(String sellerResponse, LocalDateTime processedAt) {
        validateRequested();
        this.status = ReturnRequestStatus.REJECTED;
        this.sellerResponse = sellerResponse;
        this.processedAt = processedAt;
    }

    private void validateRequested() {
        if (status != ReturnRequestStatus.REQUESTED) {
            throw new IllegalStateException("Only requested returns can be processed.");
        }
    }
}
