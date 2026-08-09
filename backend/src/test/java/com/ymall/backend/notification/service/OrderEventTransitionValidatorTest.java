package com.ymall.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.ymall.backend.global.messaging.OrderEventType;

class OrderEventTransitionValidatorTest {

    private final OrderEventTransitionValidator validator = new OrderEventTransitionValidator();

    @Test
    void acceptsValidOrderEventTransitions() {
        assertThat(validator.isAllowed(null, OrderEventType.ORDER_CREATED)).isTrue();
        assertThat(validator.isAllowed(
            OrderEventType.ORDER_CREATED, OrderEventType.PAYMENT_FAILED
        )).isTrue();
        assertThat(validator.isAllowed(
            OrderEventType.PAYMENT_FAILED, OrderEventType.PAYMENT_COMPLETED
        )).isTrue();
        assertThat(validator.isAllowed(
            OrderEventType.PAYMENT_COMPLETED, OrderEventType.ORDER_PREPARING
        )).isTrue();
        assertThat(validator.isAllowed(
            OrderEventType.ORDER_PREPARING, OrderEventType.ORDER_SHIPPED
        )).isTrue();
        assertThat(validator.isAllowed(
            OrderEventType.ORDER_SHIPPED, OrderEventType.ORDER_DELIVERED
        )).isTrue();
    }

    @Test
    void rejectsReversedAndTerminalOrderEventTransitions() {
        assertThat(validator.isAllowed(
            OrderEventType.ORDER_DELIVERED, OrderEventType.ORDER_PREPARING
        )).isFalse();
        assertThat(validator.isAllowed(
            OrderEventType.ORDER_CANCELED, OrderEventType.PAYMENT_COMPLETED
        )).isFalse();
        assertThat(validator.isAllowed(null, OrderEventType.ORDER_SHIPPED)).isFalse();
    }
}
