package com.ymall.backend.order.entity;

import java.math.BigDecimal;

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

import com.ymall.backend.product.entity.Product;

@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 255)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 22, scale = 2)
    private BigDecimal lineTotal;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OrderItemFulfillmentStatus fulfillmentStatus;

    public OrderItem(Product product, String productName, BigDecimal unitPrice, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
        }
        this.product = product;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.fulfillmentStatus = OrderItemFulfillmentStatus.PENDING;
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    public OrderItemFulfillmentStatus getEffectiveFulfillmentStatus() {
        return fulfillmentStatus == null ? OrderItemFulfillmentStatus.PENDING : fulfillmentStatus;
    }

    public void updateFulfillmentStatus(OrderItemFulfillmentStatus targetStatus) {
        OrderItemFulfillmentStatus currentStatus = getEffectiveFulfillmentStatus();
        if (currentStatus == targetStatus) {
            return;
        }
        if (targetStatus == null || targetStatus.ordinal() != currentStatus.ordinal() + 1) {
            throw new IllegalStateException("배송 상태는 한 단계씩 변경해야 합니다.");
        }
        this.fulfillmentStatus = targetStatus;
    }
}
