package com.ymall.backend.member.dto;

public record SignupEmailVerificationConfirmResponse(
    String verificationToken,
    long expiresIn
) {
}
