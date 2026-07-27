package com.ymall.backend.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SettlementRequestCreateRequest(
    @NotBlank
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])")
    String period
) {
}
