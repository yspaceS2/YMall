package com.ymall.backend.notification.service;

import java.util.EnumSet;

import org.springframework.stereotype.Component;

import com.ymall.backend.global.messaging.OrderEventType;

@Component
public class OrderEventTransitionValidator {

    public boolean isAllowed(OrderEventType previous, OrderEventType current) {
        if (previous == null) {
            return current == OrderEventType.ORDER_CREATED;
        }
        return switch (previous) {
            case ORDER_CREATED -> EnumSet.of(
                OrderEventType.PAYMENT_COMPLETED,
                OrderEventType.PAYMENT_FAILED,
                OrderEventType.ORDER_CANCELED
            ).contains(current);
            case PAYMENT_FAILED -> EnumSet.of(
                OrderEventType.PAYMENT_COMPLETED,
                OrderEventType.PAYMENT_FAILED,
                OrderEventType.ORDER_CANCELED
            ).contains(current);
            case PAYMENT_COMPLETED -> current == OrderEventType.ORDER_PREPARING;
            case ORDER_PREPARING -> current == OrderEventType.ORDER_SHIPPED;
            case ORDER_SHIPPED -> current == OrderEventType.ORDER_DELIVERED;
            case ORDER_CANCELED, ORDER_DELIVERED -> false;
        };
    }
}
