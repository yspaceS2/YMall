package com.ymall.backend.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderCreateRequest(
    @NotBlank @Size(max = 100) String idempotencyKey
) {
}
