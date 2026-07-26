package com.ymall.backend.member.dto;

public record SignupEmailVerificationResponse(
    String requestId,
    long expiresIn
) {
}
