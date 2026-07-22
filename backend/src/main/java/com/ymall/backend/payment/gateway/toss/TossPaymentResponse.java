package com.ymall.backend.payment.gateway.toss;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

record TossPaymentResponse(
    String paymentKey,
    String orderId,
    String status,
    BigDecimal totalAmount,
    BigDecimal balanceAmount,
    String method,
    OffsetDateTime approvedAt
) {
}
