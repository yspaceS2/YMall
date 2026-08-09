package com.ymall.backend.payment.gateway;

import java.math.BigDecimal;

public record PaymentCancelCommand(
    String paymentKey,
    String reason,
    BigDecimal cancelAmount,
    String idempotencyKey
) {
}
