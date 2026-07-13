package com.ymall.backend.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class JwtPropertiesTest {

    private static final String SECRET =
        "test-jwt-secret-key-for-ymall-that-is-at-least-32-bytes-long";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsZeroAccessTokenExpiration() {
        Set<ConstraintViolation<JwtProperties>> violations = validate(Duration.ZERO);

        assertThat(violations)
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("accessTokenExpiration");
    }

    @Test
    void rejectsNegativeAccessTokenExpiration() {
        Set<ConstraintViolation<JwtProperties>> violations = validate(Duration.ofSeconds(-1));

        assertThat(violations)
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("accessTokenExpiration");
    }

    @Test
    void acceptsPositiveAccessTokenExpiration() {
        assertThat(validate(Duration.ofSeconds(1))).isEmpty();
    }

    private Set<ConstraintViolation<JwtProperties>> validate(Duration expiration) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpiration(expiration);
        return validator.validate(properties);
    }
}
