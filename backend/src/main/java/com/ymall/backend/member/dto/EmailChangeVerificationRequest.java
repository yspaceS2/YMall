package com.ymall.backend.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailChangeVerificationRequest(
    @NotBlank
    @Email
    @Size(max = 255)
    String email
) {
}
