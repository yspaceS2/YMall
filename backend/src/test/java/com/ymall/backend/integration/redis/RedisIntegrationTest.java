package com.ymall.backend.integration.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RedisIntegrationTest {

    private static final String TEST_KEY = "test:redis:connection";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @AfterEach
    void tearDown() {
        redisTemplate.delete(TEST_KEY);
    }

    @Test
    void storesAndReadsJsonValueWithConfiguredSerializers() {
        RedisSample expected = new RedisSample("connected", 1);

        redisTemplate.opsForValue().set(TEST_KEY, expected, Duration.ofMinutes(1));

        assertThat(redisTemplate.opsForValue().get(TEST_KEY)).isEqualTo(expected);
        assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJacksonJsonRedisSerializer.class);
        assertThat(redisTemplate.getExpire(TEST_KEY)).isPositive();
    }

    private record RedisSample(String status, int attempt) {
    }
}
