package com.ymall.backend.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

@Component
public class RefreshTokenCookieManager {

    public static final String COOKIE_NAME = "YMALL_REFRESH_TOKEN";

    private final JwtProperties jwtProperties;
    private final boolean secure;

    public RefreshTokenCookieManager(
        JwtProperties jwtProperties,
        @Value("${ymall.jwt.refresh-token-cookie-secure:false}") boolean secure
    ) {
        this.jwtProperties = jwtProperties;
        this.secure = secure;
    }

    public void write(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(refreshToken, jwtProperties.getRefreshTokenExpiration()).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", java.time.Duration.ZERO).toString());
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie cookie(String value, java.time.Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(maxAge)
            .build();
    }
}
