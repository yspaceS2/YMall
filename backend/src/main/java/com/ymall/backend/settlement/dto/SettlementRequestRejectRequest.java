package com.ymall.backend.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SettlementRequestRejectRequest(
    @NotBlank
    @Size(max = 500)
    String reason
) {
}
