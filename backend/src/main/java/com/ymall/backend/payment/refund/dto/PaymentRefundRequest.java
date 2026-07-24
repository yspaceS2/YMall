package com.ymall.backend.payment.refund.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentRefundRequest(
    @NotBlank @Size(max = 100) String idempotencyKey,
    @NotBlank @Size(max = 200) String reason,
    List<@Valid PaymentRefundItemRequest> items
) {
}
