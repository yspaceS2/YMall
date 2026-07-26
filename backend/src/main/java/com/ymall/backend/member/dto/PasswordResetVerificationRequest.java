package com.ymall.backend.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetVerificationRequest(
    @NotBlank @Size(min = 20, max = 200) String requestId,
    @NotBlank @Pattern(regexp = "\\d{6}") String code
) {
}
