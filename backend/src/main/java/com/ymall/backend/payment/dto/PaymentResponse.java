package com.ymall.backend.payment.dto;

import java.time.LocalDateTime;

import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.payment.entity.PaymentResult;

public record PaymentResponse(
    Long paymentId,
    Long orderId,
    PaymentResult result,
    OrderStatus orderStatus,
    String failureMessage,
    LocalDateTime processedAt
) {
}
