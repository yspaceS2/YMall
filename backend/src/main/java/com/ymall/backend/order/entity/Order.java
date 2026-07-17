package com.ymall.backend.order.entity;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Order(Member member, String idempotencyKey) {
        this.member = member;
        this.idempotencyKey = idempotencyKey;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.totalAmount = BigDecimal.ZERO.setScale(2);
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

    public void refreshFulfillmentStatus() {
        if (items.stream().allMatch(item ->
            item.getEffectiveFulfillmentStatus() == OrderItemFulfillmentStatus.DELIVERED
        )) {
            this.status = OrderStatus.DELIVERED;
            return;
        }
        if (items.stream().allMatch(item -> {
            OrderItemFulfillmentStatus itemStatus = item.getEffectiveFulfillmentStatus();
            return itemStatus == OrderItemFulfillmentStatus.SHIPPED
                || itemStatus == OrderItemFulfillmentStatus.DELIVERED;
        })) {
            this.status = OrderStatus.SHIPPED;
            return;
        }
        if (items.stream().anyMatch(item ->
            item.getEffectiveFulfillmentStatus() != OrderItemFulfillmentStatus.PENDING
        )) {
            this.status = OrderStatus.PREPARING;
            return;
        }
        this.status = OrderStatus.PAID;
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
