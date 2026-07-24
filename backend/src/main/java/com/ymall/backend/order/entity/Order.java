package com.ymall.backend.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import com.ymall.backend.member.entity.Member;

@Getter
@Entity
@Table(
    name = "orders",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_orders_member_idempotency_key",
        columnNames = {"member_id", "idempotency_key"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "payment_order_id", nullable = false, unique = true, updatable = false, length = 64)
    private String paymentOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal totalAmount;

    @Embedded
    private DeliveryAddressSnapshot deliveryAddress;

    @Column(name = "inventory_reserved", nullable = false)
    private boolean inventoryReserved;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Order(Member member, String idempotencyKey) {
        this(member, idempotencyKey, null);
    }

    public Order(Member member, String idempotencyKey, DeliveryAddressSnapshot deliveryAddress) {
        this.member = member;
        this.idempotencyKey = idempotencyKey;
        this.paymentOrderId = "YMALL-" + UUID.randomUUID().toString().replace("-", "");
        this.status = OrderStatus.PENDING_PAYMENT;
        this.totalAmount = BigDecimal.ZERO.setScale(2);
        this.deliveryAddress = deliveryAddress;
        this.inventoryReserved = true;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.assignOrder(this);
        totalAmount = totalAmount.add(item.getLineTotal());
    }

    public void completePayment() {
        this.status = OrderStatus.PAID;
    }

    public void failPayment() {
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELED;
    }

    public void applyRefund(boolean fullyRefunded) {
        this.status = fullyRefunded
            ? OrderStatus.REFUNDED
            : OrderStatus.PARTIALLY_REFUNDED;
        if (fullyRefunded) {
            this.inventoryReserved = false;
        }
    }

    public void reserveInventory() {
        this.inventoryReserved = true;
    }

    public void releaseInventory() {
        this.inventoryReserved = false;
    }

    public void refreshFulfillmentStatus() {
        List<OrderItem> activeItems = items.stream()
            .filter(item -> item.getRefundableQuantity() > 0)
            .toList();
        if (activeItems.isEmpty()) {
            return;
        }
        if (activeItems.stream().allMatch(item ->
            item.getEffectiveFulfillmentStatus() == OrderItemFulfillmentStatus.DELIVERED
        )) {
            this.status = OrderStatus.DELIVERED;
            return;
        }
        if (activeItems.stream().allMatch(item -> {
            OrderItemFulfillmentStatus itemStatus = item.getEffectiveFulfillmentStatus();
            return itemStatus == OrderItemFulfillmentStatus.SHIPPED
                || itemStatus == OrderItemFulfillmentStatus.DELIVERED;
        })) {
            this.status = OrderStatus.SHIPPED;
            return;
        }
        if (activeItems.stream().anyMatch(item ->
            item.getEffectiveFulfillmentStatus() != OrderItemFulfillmentStatus.PENDING
        )) {
            this.status = OrderStatus.PREPARING;
            return;
        }
        if (status != OrderStatus.PARTIALLY_REFUNDED) {
            this.status = OrderStatus.PAID;
        }
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
