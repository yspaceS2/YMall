package com.ymall.backend.member.dto;

public record PasswordResetVerificationResponse(
    String resetToken,
    long expiresIn
) {
}
