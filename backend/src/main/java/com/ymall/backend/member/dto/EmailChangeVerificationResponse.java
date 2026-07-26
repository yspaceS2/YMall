package com.ymall.backend.member.dto;

public record EmailChangeVerificationResponse(
    String requestId,
    long expiresIn
) {
}
