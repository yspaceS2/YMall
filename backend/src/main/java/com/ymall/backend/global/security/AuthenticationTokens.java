package com.ymall.backend.global.security;

import com.ymall.backend.member.dto.TokenResponse;

public record AuthenticationTokens(TokenResponse accessToken, String refreshToken) {
}
