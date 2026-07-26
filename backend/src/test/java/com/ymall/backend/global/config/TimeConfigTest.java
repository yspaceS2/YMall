package com.ymall.backend.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class TimeConfigTest {

    @Test
    void configuresUtcClockAndDefaultTimeZone() {
        TimeConfig config = new TimeConfig();

        Clock clock = config.clock();

        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
    }

    @Test
    void serializesLocalDateTimeWithUtcOffset() throws Exception {
        TimeConfig config = new TimeConfig();
        JsonMapper.Builder builder = JsonMapper.builder();
        config.utcJsonMapperCustomizer().customize(builder);
        ObjectMapper objectMapper = builder.build();

        String json = objectMapper.writeValueAsString(Map.of(
            "createdAt",
            LocalDateTime.of(2026, 7, 26, 12, 0)
        ));

        assertThat(json).contains("\"createdAt\":\"2026-07-26T12:00:00Z\"");
    }
}
