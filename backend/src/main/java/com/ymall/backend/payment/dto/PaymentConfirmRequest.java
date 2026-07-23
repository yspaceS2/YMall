package com.ymall.backend.payment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentConfirmRequest(
    @NotBlank @Size(max = 200) String paymentKey,
    @NotBlank @Size(max = 64) String paymentOrderId,
    @NotNull @DecimalMin("1") BigDecimal amount,
    @NotBlank @Size(max = 100) String idempotencyKey
) {
}
