package com.ymall.backend.order.returnrequest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReturnRequestCreateRequest(
    @NotNull Long orderItemId,
    @Min(1) @Max(999) int quantity,
    @NotBlank @Size(max = 500) String reason
) {
}
