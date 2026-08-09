package com.ymall.backend.payment.gateway;

import java.math.BigDecimal;

public record PaymentConfirmCommand(
    String paymentKey,
    String orderId,
    BigDecimal amount,
    String idempotencyKey
) {
}
