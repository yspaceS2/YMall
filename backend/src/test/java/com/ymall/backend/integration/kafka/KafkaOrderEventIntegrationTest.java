package com.ymall.backend.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.global.messaging.OrderEventProducer;
import com.ymall.backend.global.messaging.OrderEventType;

@SpringBootTest(properties = "ymall.kafka.enabled=true")
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 3,
    topics = "ymall.order.events.v1",
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class KafkaOrderEventIntegrationTest {

    @Autowired private OrderEventProducer producer;
    @Autowired private TestOrderEventListener listener;

    @Test
    void producerAndConsumerExchangeOrderEvent() throws Exception {
        OrderEventEnvelope expected = OrderEventEnvelope.create(
            OrderEventType.ORDER_CREATED,
            101L,
            20L,
            Map.of("totalAmount", 39000)
        );

        producer.send(expected).get(10, TimeUnit.SECONDS);
        OrderEventEnvelope consumed = listener.poll(expected, 10, TimeUnit.SECONDS);

        assertThat(consumed).isEqualTo(expected);
    }

    @Test
    void routesEventsForSameOrderToSamePartition() throws Exception {
        OrderEventEnvelope created = OrderEventEnvelope.create(
            OrderEventType.ORDER_CREATED, 202L, 20L, Map.of()
        );
        OrderEventEnvelope paid = OrderEventEnvelope.create(
            OrderEventType.PAYMENT_COMPLETED, 202L, 20L, Map.of()
        );

        int createdPartition = producer.send(created)
            .get(10, TimeUnit.SECONDS)
            .getRecordMetadata()
            .partition();
        int paidPartition = producer.send(paid)
            .get(10, TimeUnit.SECONDS)
            .getRecordMetadata()
            .partition();

        assertThat(paidPartition).isEqualTo(createdPartition);
    }

    @TestConfiguration
    static class ListenerConfiguration {

        @Bean
        TestOrderEventListener testOrderEventListener() {
            return new TestOrderEventListener();
        }
    }

    static class TestOrderEventListener {

        private final BlockingQueue<OrderEventEnvelope> events = new LinkedBlockingQueue<>();

        @KafkaListener(
            topics = "${ymall.kafka.order-events.name}",
            groupId = "ymall-kafka-integration-test"
        )
        void consume(OrderEventEnvelope event) {
            events.add(event);
        }

        OrderEventEnvelope poll(
            OrderEventEnvelope expected,
            long timeout,
            TimeUnit timeUnit
        ) throws InterruptedException {
            long deadline = System.nanoTime() + timeUnit.toNanos(timeout);
            while (System.nanoTime() < deadline) {
                OrderEventEnvelope event = events.poll(100, TimeUnit.MILLISECONDS);
                if (event != null && event.eventId().equals(expected.eventId())) {
                    return event;
                }
            }
            return null;
        }
    }
}
