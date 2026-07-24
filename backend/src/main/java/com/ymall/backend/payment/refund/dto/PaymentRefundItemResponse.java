package com.ymall.backend.payment.refund.dto;

import java.math.BigDecimal;

public record PaymentRefundItemResponse(
    Long orderItemId,
    String productName,
    int quantity,
    BigDecimal amount
) {
}
