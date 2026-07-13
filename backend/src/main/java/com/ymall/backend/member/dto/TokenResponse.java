package com.ymall.backend.member.dto;

public record TokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {
}
