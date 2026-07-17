package com.ymall.backend.notification.event;

import com.ymall.backend.notification.entity.NotificationType;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.payment.entity.PaymentResult;

public record NotificationEvent(
    Long memberId,
    NotificationType type,
    String title,
    String message,
    String targetUrl
) {

    public static NotificationEvent orderCreated(Long memberId, Long orderId) {
        return orderEvent(
            memberId,
            orderId,
            NotificationType.ORDER_CREATED,
            "주문이 생성되었습니다.",
            "주문 #%d의 결제를 진행해 주세요.".formatted(orderId)
        );
    }

    public static NotificationEvent paymentProcessed(
        Long memberId,
        Long orderId,
        PaymentResult result
    ) {
        if (result == PaymentResult.SUCCESS) {
            return orderEvent(
                memberId,
                orderId,
                NotificationType.PAYMENT_COMPLETED,
                "결제가 완료되었습니다.",
                "주문 #%d의 결제가 완료되었습니다.".formatted(orderId)
            );
        }
        return orderEvent(
            memberId,
            orderId,
            NotificationType.PAYMENT_FAILED,
            "결제에 실패했습니다.",
            "주문 #%d의 결제를 다시 시도해 주세요.".formatted(orderId)
        );
    }

    public static NotificationEvent orderCanceled(Long memberId, Long orderId) {
        return orderEvent(
            memberId,
            orderId,
            NotificationType.ORDER_CANCELED,
            "주문이 취소되었습니다.",
            "주문 #%d이 취소되었습니다.".formatted(orderId)
        );
    }

    public static NotificationEvent fulfillmentChanged(
        Long memberId,
        Long orderId,
        OrderItemFulfillmentStatus status
    ) {
        return switch (status) {
            case PREPARING -> orderEvent(
                memberId,
                orderId,
                NotificationType.ORDER_PREPARING,
                "상품 준비가 시작되었습니다.",
                "주문 #%d의 상품을 준비하고 있습니다.".formatted(orderId)
            );
            case SHIPPED -> orderEvent(
                memberId,
                orderId,
                NotificationType.ORDER_SHIPPED,
                "배송이 시작되었습니다.",
                "주문 #%d의 상품이 배송 중입니다.".formatted(orderId)
            );
            case DELIVERED -> orderEvent(
                memberId,
                orderId,
                NotificationType.ORDER_DELIVERED,
                "배송이 완료되었습니다.",
                "주문 #%d의 상품 배송이 완료되었습니다.".formatted(orderId)
            );
            case PENDING -> throw new IllegalArgumentException("대기 상태 알림은 발행할 수 없습니다.");
        };
    }

    private static NotificationEvent orderEvent(
        Long memberId,
        Long orderId,
        NotificationType type,
        String title,
        String message
    ) {
        return new NotificationEvent(
            memberId,
            type,
            title,
            message,
            "/orders/%d/result".formatted(orderId)
        );
    }
}
