package com.ymall.backend.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupEmailVerificationRequest(
    @NotBlank @Email @Size(max = 255) String email
) {

    public SignupEmailVerificationRequest {
        email = email == null ? null : email.trim();
    }
}
