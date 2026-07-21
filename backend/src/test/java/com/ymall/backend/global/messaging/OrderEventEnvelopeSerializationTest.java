package com.ymall.backend.global.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class OrderEventEnvelopeSerializationTest {

    @Autowired private ObjectMapper objectMapper;

    @Test
    void serializesAndDeserializesVersionOneEnvelope() throws Exception {
        OrderEventEnvelope expected = new OrderEventEnvelope(
            UUID.fromString("d154e893-a46b-46e6-a1ae-14a8ec2317eb"),
            OrderEventType.PAYMENT_COMPLETED,
            Instant.parse("2026-07-21T00:00:00Z"),
            100L,
            20L,
            Map.of("status", "PAID", "paymentId", 10L),
            1
        );

        String json = objectMapper.writeValueAsString(expected);
        OrderEventEnvelope restored = objectMapper.readValue(json, OrderEventEnvelope.class);

        assertThat(restored.eventId()).isEqualTo(expected.eventId());
        assertThat(restored.eventType()).isEqualTo(expected.eventType());
        assertThat(restored.occurredAt()).isEqualTo(expected.occurredAt());
        assertThat(restored.orderId()).isEqualTo(expected.orderId());
        assertThat(restored.memberId()).isEqualTo(expected.memberId());
        assertThat(restored.payload().get("status")).isEqualTo("PAID");
        assertThat(((Number) restored.payload().get("paymentId")).longValue()).isEqualTo(10L);
        assertThat(restored.version()).isEqualTo(expected.version());
        assertThat(json).contains(
            "\"eventId\"",
            "\"eventType\"",
            "\"occurredAt\"",
            "\"orderId\"",
            "\"memberId\"",
            "\"payload\"",
            "\"version\""
        );
    }

    @Test
    void acceptsAdditionalPayloadFieldsWithoutChangingEnvelopeVersion() throws Exception {
        String json = """
            {
              "eventId":"d154e893-a46b-46e6-a1ae-14a8ec2317eb",
              "eventType":"ORDER_CREATED",
              "occurredAt":"2026-07-21T00:00:00Z",
              "orderId":100,
              "memberId":20,
              "payload":{"totalAmount":39000,"futureField":"compatible"},
              "version":1
            }
            """;

        OrderEventEnvelope event = objectMapper.readValue(json, OrderEventEnvelope.class);

        assertThat(event.version()).isEqualTo(1);
        assertThat(event.payload().get("futureField")).isEqualTo("compatible");
    }
}
