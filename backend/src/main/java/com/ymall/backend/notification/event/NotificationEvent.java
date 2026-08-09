package com.ymall.backend.notification.event;

import java.util.UUID;

import com.ymall.backend.notification.entity.NotificationType;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.payment.entity.PaymentResult;

public record NotificationEvent(
    UUID sourceEventId,
    Long memberId,
    NotificationType type,
    String title,
    String message,
    String targetUrl
) {

    public static NotificationEvent orderCreated(UUID eventId, Long memberId, Long orderId) {
        return orderEvent(
            eventId,
            memberId,
            orderId,
            NotificationType.ORDER_CREATED,
            "주문이 생성되었습니다.",
            "주문 #%d의 결제를 진행해 주세요.".formatted(orderId)
        );
    }

    public static NotificationEvent paymentProcessed(
        UUID eventId,
        Long memberId,
        Long orderId,
        PaymentResult result
    ) {
        if (result == PaymentResult.SUCCESS) {
            return orderEvent(
                eventId,
                memberId,
                orderId,
                NotificationType.PAYMENT_COMPLETED,
                "결제가 완료되었습니다.",
                "주문 #%d의 결제가 완료되었습니다.".formatted(orderId)
            );
        }
        return orderEvent(
            eventId,
            memberId,
            orderId,
            NotificationType.PAYMENT_FAILED,
            "결제에 실패했습니다.",
            "주문 #%d의 결제를 다시 시도해 주세요.".formatted(orderId)
        );
    }

    public static NotificationEvent orderCanceled(UUID eventId, Long memberId, Long orderId) {
        return orderEvent(
            eventId,
            memberId,
            orderId,
            NotificationType.ORDER_CANCELED,
            "주문이 취소되었습니다.",
            "주문 #%d이 취소되었습니다.".formatted(orderId)
        );
    }

    public static NotificationEvent fulfillmentChanged(
        UUID eventId,
        Long memberId,
        Long orderId,
        OrderItemFulfillmentStatus status
    ) {
        return switch (status) {
            case PREPARING -> orderEvent(
                eventId,
                memberId,
                orderId,
                NotificationType.ORDER_PREPARING,
                "상품 준비가 시작되었습니다.",
                "주문 #%d의 상품을 준비하고 있습니다.".formatted(orderId)
            );
            case SHIPPED -> orderEvent(
                eventId,
                memberId,
                orderId,
                NotificationType.ORDER_SHIPPED,
                "배송이 시작되었습니다.",
                "주문 #%d의 상품이 배송 중입니다.".formatted(orderId)
            );
            case DELIVERED -> orderEvent(
                eventId,
                memberId,
                orderId,
                NotificationType.ORDER_DELIVERED,
                "배송이 완료되었습니다.",
                "주문 #%d의 상품 배송이 완료되었습니다.".formatted(orderId)
            );
            case PENDING -> throw new IllegalArgumentException("대기 상태 알림은 발행할 수 없습니다.");
        };
    }

    public static NotificationEvent returnRequested(
        UUID eventId,
        Long sellerMemberId,
        Long returnRequestId,
        String productName
    ) {
        return new NotificationEvent(
            eventId,
            sellerMemberId,
            NotificationType.RETURN_REQUESTED,
            "새 반품 요청이 접수되었습니다.",
            "%s 상품의 반품 요청을 확인해 주세요.".formatted(productName),
            "/seller/returns/%d".formatted(returnRequestId)
        );
    }

    public static NotificationEvent returnProcessed(
        UUID eventId,
        Long memberId,
        Long orderId,
        String productName,
        boolean approved
    ) {
        return new NotificationEvent(
            eventId,
            memberId,
            approved
                ? NotificationType.RETURN_APPROVED
                : NotificationType.RETURN_REJECTED,
            approved ? "반품이 승인되었습니다." : "반품 요청이 거절되었습니다.",
            approved
                ? "%s 상품의 환불 처리가 완료되었습니다.".formatted(productName)
                : "%s 상품의 반품 처리 결과를 확인해 주세요.".formatted(productName),
            "/mypage/orders"
        );
    }

    private static NotificationEvent orderEvent(
        UUID eventId,
        Long memberId,
        Long orderId,
        NotificationType type,
        String title,
        String message
    ) {
        return new NotificationEvent(
            eventId,
            memberId,
            type,
            title,
            message,
            "/orders/%d/result".formatted(orderId)
        );
    }
}
