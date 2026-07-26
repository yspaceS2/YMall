package com.ymall.backend.member.dto;

public record GoogleOneTapNonceResponse(
    String clientId,
    String nonce,
    long expiresIn
) {
}
