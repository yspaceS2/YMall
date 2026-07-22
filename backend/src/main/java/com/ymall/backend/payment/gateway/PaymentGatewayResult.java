package com.ymall.backend.payment.gateway;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentGatewayResult(
    String paymentKey,
    String orderId,
    PaymentGatewayStatus status,
    BigDecimal totalAmount,
    BigDecimal balanceAmount,
    String method,
    OffsetDateTime approvedAt
) {
}
