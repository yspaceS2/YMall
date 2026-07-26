package com.ymall.backend.global.config;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class GoogleOneTapConfig {

    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS = Set.of(
        "accounts.google.com",
        "https://accounts.google.com"
    );

    @Bean("googleOneTapJwtDecoder")
    public JwtDecoder googleOneTapJwtDecoder(
        @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(List.of(
            issuerValidator(),
            audienceValidator(clientId)
        )));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> issuerValidator() {
        return jwt -> jwt.getIssuer() != null
            && GOOGLE_ISSUERS.contains(jwt.getIssuer().toString())
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(invalidToken("Google ID token issuer is invalid"));
    }

    private OAuth2TokenValidator<Jwt> audienceValidator(String clientId) {
        return jwt -> jwt.getAudience().contains(clientId)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(invalidToken("Google ID token audience is invalid"));
    }

    private OAuth2Error invalidToken(String description) {
        return new OAuth2Error("invalid_token", description, null);
    }
}
