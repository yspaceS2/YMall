package com.ymall.backend.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.payment.entity.PaymentResult;

public record PaymentResponse(
    Long paymentId,
    Long orderId,
    String paymentKey,
    String paymentOrderId,
    BigDecimal requestedAmount,
    BigDecimal approvedAmount,
    String method,
    OffsetDateTime approvedAt,
    PaymentResult result,
    OrderStatus orderStatus,
    String failureCode,
    String failureMessage,
    LocalDateTime processedAt
) {
}
