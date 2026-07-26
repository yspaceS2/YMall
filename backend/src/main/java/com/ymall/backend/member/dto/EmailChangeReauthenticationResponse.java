package com.ymall.backend.member.dto;

public record EmailChangeReauthenticationResponse(
    boolean verificationRequired,
    String requestId,
    String maskedEmail,
    long expiresIn
) {
}
