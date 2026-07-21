package com.ymall.backend.notification.event;

import org.springframework.stereotype.Component;

import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.payment.entity.PaymentResult;

@Component
public class OrderNotificationEventMapper {

    public NotificationEvent map(OrderEventEnvelope event) {
        return switch (event.eventType()) {
            case ORDER_CREATED -> NotificationEvent.orderCreated(
                event.eventId(), event.memberId(), event.orderId()
            );
            case PAYMENT_COMPLETED -> NotificationEvent.paymentProcessed(
                event.eventId(), event.memberId(), event.orderId(), PaymentResult.SUCCESS
            );
            case PAYMENT_FAILED -> NotificationEvent.paymentProcessed(
                event.eventId(), event.memberId(), event.orderId(), PaymentResult.FAILURE
            );
            case ORDER_CANCELED -> NotificationEvent.orderCanceled(
                event.eventId(), event.memberId(), event.orderId()
            );
            case ORDER_PREPARING -> fulfillmentChanged(
                event, OrderItemFulfillmentStatus.PREPARING
            );
            case ORDER_SHIPPED -> fulfillmentChanged(
                event, OrderItemFulfillmentStatus.SHIPPED
            );
            case ORDER_DELIVERED -> fulfillmentChanged(
                event, OrderItemFulfillmentStatus.DELIVERED
            );
        };
    }

    private NotificationEvent fulfillmentChanged(
        OrderEventEnvelope event,
        OrderItemFulfillmentStatus status
    ) {
        return NotificationEvent.fulfillmentChanged(
            event.eventId(), event.memberId(), event.orderId(), status
        );
    }
}
