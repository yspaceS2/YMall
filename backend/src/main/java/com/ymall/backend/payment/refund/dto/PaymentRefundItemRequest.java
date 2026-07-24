package com.ymall.backend.payment.refund.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PaymentRefundItemRequest(
    @NotNull Long orderItemId,
    @Min(1) int quantity
) {
}
