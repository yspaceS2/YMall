package com.ymall.backend.payment.refund.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ymall.backend.payment.refund.entity.PaymentRefundStatus;
import com.ymall.backend.payment.refund.entity.PaymentRefundType;

public record PaymentRefundResponse(
    Long refundId,
    Long orderId,
    PaymentRefundType type,
    PaymentRefundStatus status,
    BigDecimal amount,
    String reason,
    String failureMessage,
    List<PaymentRefundItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime processedAt
) {
}
