package com.ymall.backend.payment.webhook.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TossPaymentWebhookRequest(
    @NotBlank
    String eventType,

    @NotNull
    LocalDateTime createdAt,

    @NotNull
    @Valid
    PaymentData data
) {

    public record PaymentData(
        @NotBlank
        @Size(max = 200)
        String paymentKey,

        @NotBlank
        @Size(max = 64)
        String orderId,

        @NotBlank
        String status,

        @NotNull
        @PositiveOrZero
        BigDecimal totalAmount
    ) {
    }
}
