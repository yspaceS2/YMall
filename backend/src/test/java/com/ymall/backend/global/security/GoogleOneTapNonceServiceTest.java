package com.ymall.backend.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class GoogleOneTapNonceServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private GoogleOneTapNonceService service;

    @BeforeEach
    void setUp() {
        GoogleOneTapProperties properties = new GoogleOneTapProperties();
        properties.setNonceTtl(Duration.ofMinutes(5));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        service = new GoogleOneTapNonceService(redisTemplate, properties);
    }

    @Test
    void issuesRandomNonceAndStoresOnlyItsDigest() {
        String nonce = service.issue();

        assertThat(nonce).isNotBlank();
        verify(valueOperations).set(
            org.mockito.ArgumentMatchers.startsWith("auth:google-one-tap:nonce:"),
            org.mockito.ArgumentMatchers.eq("1"),
            org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(5))
        );
    }

    @Test
    void consumesNonceOnlyOnce() {
        given(valueOperations.getAndDelete(anyString()))
            .willReturn("1")
            .willReturn(null);

        service.consume("one-time-nonce");

        assertThatThrownBy(() -> service.consume("one-time-nonce"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.GOOGLE_ONE_TAP_TOKEN_INVALID);
    }
}
