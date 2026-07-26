package com.ymall.backend.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupEmailVerificationConfirmRequest(
    @NotBlank @Size(max = 255) String requestId,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Pattern(regexp = "\\d{6}") String code
) {

    public SignupEmailVerificationConfirmRequest {
        email = email == null ? null : email.trim();
    }
}
