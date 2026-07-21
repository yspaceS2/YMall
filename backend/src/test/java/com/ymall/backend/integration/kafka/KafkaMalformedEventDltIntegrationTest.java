package com.ymall.backend.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import com.ymall.backend.global.messaging.DeadLetterByteKafkaOperations;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ymall-kafka-malformed-dlt-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "ymall.kafka.enabled=true",
    "ymall.kafka.notification-consumer.enabled=true",
    "ymall.kafka.notification-consumer.group-id=ymall-malformed-dlt-integration-test",
    "ymall.kafka.consumer-retry.max-retries=2",
    "ymall.kafka.consumer-retry.retry-delay=10ms"
})
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 3,
    topics = {"ymall.order.events.v1", "ymall.order.events.v1.DLT"},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class KafkaMalformedEventDltIntegrationTest {

    private static final String ORDER_EVENTS_TOPIC = "ymall.order.events.v1";
    private static final String DLT_TOPIC = ORDER_EVENTS_TOPIC + ".DLT";

    @Autowired
    private DeadLetterByteKafkaOperations deadLetterByteKafkaOperations;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void sendsMalformedJsonDirectlyToDltWithoutChangingOriginalValue() throws Exception {
        byte[] malformedJson = "{not-valid-json".getBytes(StandardCharsets.UTF_8);

        try (Consumer<String, byte[]> consumer = createDltConsumer()) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, DLT_TOPIC);
            deadLetterByteKafkaOperations.kafkaTemplate()
                .send(ORDER_EVENTS_TOPIC, "901", malformedJson)
                .get(10, TimeUnit.SECONDS);

            ConsumerRecord<String, byte[]> deadLetter = KafkaTestUtils.getSingleRecord(
                consumer,
                DLT_TOPIC,
                Duration.ofSeconds(10)
            );

            assertThat(deadLetter.key()).isEqualTo("901");
            assertThat(deadLetter.value()).isEqualTo(malformedJson);
        }
    }

    private Consumer<String, byte[]> createDltConsumer() {
        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(
            embeddedKafkaBroker,
            "ymall-malformed-dlt-verifier",
            false
        );
        return new DefaultKafkaConsumerFactory<>(
            consumerProperties,
            new StringDeserializer(),
            new ByteArrayDeserializer()
        ).createConsumer();
    }
}
