package com.ymall.backend.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class GoogleOneTapTokenVerifierTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private GoogleOneTapNonceService nonceService;

    private GoogleOneTapTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new GoogleOneTapTokenVerifier(jwtDecoder, nonceService);
    }

    @Test
    void verifiesGoogleClaimsAndConsumesNonce() {
        Jwt jwt = jwt(Map.of(
            "sub", "google-user",
            "email", " User@Example.com ",
            "email_verified", true,
            "name", "Google User",
            "nonce", "one-time-nonce"
        ));
        given(jwtDecoder.decode("google-credential")).willReturn(jwt);

        OAuth2UserProfile profile = verifier.verify("google-credential");

        assertThat(profile.providerUserId()).isEqualTo("google-user");
        assertThat(profile.email()).isEqualTo("user@example.com");
        assertThat(profile.name()).isEqualTo("Google User");
        verify(nonceService).consume("one-time-nonce");
    }

    @Test
    void rejectsUnverifiedEmailBeforeConsumingNonce() {
        Jwt jwt = jwt(Map.of(
            "sub", "google-user",
            "email", "user@example.com",
            "email_verified", false,
            "nonce", "one-time-nonce"
        ));
        given(jwtDecoder.decode("google-credential")).willReturn(jwt);

        assertInvalidToken(() -> verifier.verify("google-credential"));
        verify(nonceService, never()).consume("one-time-nonce");
    }

    @Test
    void rejectsCredentialWhenSignatureValidationFails() {
        given(jwtDecoder.decode("invalid-credential"))
            .willThrow(new BadJwtException("invalid signature"));

        assertInvalidToken(() -> verifier.verify("invalid-credential"));
        verify(nonceService, never()).consume(org.mockito.ArgumentMatchers.any());
    }

    private Jwt jwt(Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("credential")
            .header("alg", "RS256")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .claims(values -> values.putAll(claims))
            .build();
    }

    private void assertInvalidToken(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.GOOGLE_ONE_TAP_TOKEN_INVALID);
    }
}
