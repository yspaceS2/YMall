package com.ymall.backend.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ymall.backend.payment.entity.PaymentResult;

public record MockPaymentRequest(
    @NotBlank @Size(max = 100) String idempotencyKey,
    @NotNull PaymentResult result
) {
}
