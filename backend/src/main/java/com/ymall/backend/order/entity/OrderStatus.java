package com.ymall.backend.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELED,
    PARTIALLY_REFUNDED,
    REFUNDED,
    PREPARING,
    SHIPPED,
    DELIVERED
}
