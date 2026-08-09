package com.ymall.backend.global.messaging;

public enum OrderEventType {
    ORDER_CREATED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    ORDER_CANCELED,
    ORDER_PREPARING,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    REFUND_COMPLETED
}
