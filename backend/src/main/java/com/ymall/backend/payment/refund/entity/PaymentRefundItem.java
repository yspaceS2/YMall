package com.ymall.backend.payment.refund.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import com.ymall.backend.order.entity.OrderItem;

@Getter
@Entity
@Table(name = "payment_refund_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRefundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id", nullable = false)
    private PaymentRefund refund;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    public PaymentRefundItem(OrderItem orderItem, int quantity) {
        this.orderItem = orderItem;
        this.quantity = quantity;
        this.amount = orderItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
    }

    void assignRefund(PaymentRefund refund) {
        this.refund = refund;
    }
}
