package com.ymall.backend.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELED,
    PREPARING,
    SHIPPED,
    DELIVERED
}
