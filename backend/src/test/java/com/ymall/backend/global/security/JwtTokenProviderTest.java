package com.ymall.backend.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.dto.TokenResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;

class JwtTokenProviderTest {

    private static final String SECRET =
        "test-jwt-secret-key-for-ymall-that-is-at-least-32-bytes-long";

    @Test
    void createsAndParsesAccessToken() {
        JwtTokenProvider tokenProvider = tokenProvider(Duration.ofMinutes(30));
        Member member = member();

        TokenResponse response = tokenProvider.createAccessToken(member);
        MemberPrincipal principal = tokenProvider.parseAccessToken(response.accessToken());

        assertThat(principal.memberId()).isEqualTo(1L);
        assertThat(principal.email()).isEqualTo("user@example.com");
        assertThat(principal.role()).isEqualTo(MemberRole.ROLE_USER);
        assertThat(principal.authVersion()).isZero();
        assertThat(response.expiresIn()).isEqualTo(1800);
    }

    @Test
    void parsesLegacyAccessTokenWithoutAuthVersionAsVersionZero() {
        JwtTokenProvider tokenProvider = tokenProvider(Duration.ofMinutes(30));
        Instant now = Instant.now();
        SecretKey signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
            .subject("1")
            .claim("email", "user@example.com")
            .claim("role", MemberRole.ROLE_USER.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(Duration.ofMinutes(30))))
            .signWith(signingKey)
            .compact();

        MemberPrincipal principal = tokenProvider.parseAccessToken(token);

        assertThat(principal.authVersion()).isZero();
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenProvider tokenProvider = tokenProvider(Duration.ofMinutes(30));
        String token = tokenProvider.createAccessToken(member()).accessToken();

        assertThatThrownBy(() -> tokenProvider.parseAccessToken(token + "tampered"))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider tokenProvider = tokenProvider(Duration.ofSeconds(-1));
        String token = tokenProvider.createAccessToken(member()).accessToken();

        assertThatThrownBy(() -> tokenProvider.parseAccessToken(token))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.EXPIRED_TOKEN);
    }

    private JwtTokenProvider tokenProvider(Duration expiration) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpiration(expiration);
        return new JwtTokenProvider(properties, Clock.systemUTC());
    }

    private Member member() {
        Member member = new Member(
            "user@example.com",
            "encoded-password",
            "홍길동",
            MemberRole.ROLE_USER
        );
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }
}
