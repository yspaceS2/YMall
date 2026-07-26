package com.ymall.backend.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.dto.TokenResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;

@Component
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE = "Bearer";

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final Clock clock;

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenResponse createAccessToken(Member member) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getAccessTokenExpiration());
        String token = Jwts.builder()
            .subject(member.getId().toString())
            .claim("email", member.getEmail())
            .claim(ROLE_CLAIM, member.getRole().name())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact();

        return new TokenResponse(
            token,
            TOKEN_TYPE,
            properties.getAccessTokenExpiration().toSeconds()
        );
    }

    public MemberPrincipal parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            return new MemberPrincipal(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                MemberRole.valueOf(claims.get(ROLE_CLAIM, String.class))
            );
        } catch (ExpiredJwtException exception) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
