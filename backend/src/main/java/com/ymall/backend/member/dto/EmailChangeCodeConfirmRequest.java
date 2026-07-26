package com.ymall.backend.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailChangeCodeConfirmRequest(
    @NotBlank
    String requestId,

    @NotBlank
    @Pattern(regexp = "\\d{6}")
    String code
) {
}
