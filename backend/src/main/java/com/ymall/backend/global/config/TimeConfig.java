package com.ymall.backend.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.module.SimpleModule;

@Configuration
public class TimeConfig {

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone(ZoneOffset.UTC);

    public TimeConfig() {
        TimeZone.setDefault(UTC_TIME_ZONE);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public JsonMapperBuilderCustomizer utcJsonMapperCustomizer() {
        SimpleModule module = new SimpleModule("ymall-utc-date-time");
        module.addSerializer(LocalDateTime.class, new UtcLocalDateTimeSerializer());
        return builder -> builder
            .defaultTimeZone(UTC_TIME_ZONE)
            .addModule(module);
    }
}
