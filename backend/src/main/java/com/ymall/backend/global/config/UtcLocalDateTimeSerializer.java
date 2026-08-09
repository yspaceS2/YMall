package com.ymall.backend.global.config;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class UtcLocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

    @Override
    public void serialize(
        LocalDateTime value,
        JsonGenerator generator,
        SerializationContext context
    ) throws JacksonException {
        generator.writeString(
            value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
    }
}
