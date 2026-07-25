package com.ymall.backend.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.global.messaging.KafkaMessagingMetrics;
import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.global.messaging.OrderEventProducer;
import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.global.messaging.outbox.OrderOutboxEvent;
import com.ymall.backend.global.messaging.outbox.OrderOutboxEventRepository;
import com.ymall.backend.global.messaging.outbox.OrderOutboxProperties;
import com.ymall.backend.global.messaging.outbox.OrderOutboxRelay;
import com.ymall.backend.global.messaging.outbox.OrderOutboxService;
import com.ymall.backend.global.messaging.outbox.OutboxEventStatus;

@SpringBootTest
@ActiveProfiles("test")
class OrderOutboxIntegrationTest {

    @Autowired private OrderOutboxService outboxService;
    @Autowired private OrderOutboxEventRepository outboxEventRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        outboxEventRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void rollsBackOutboxEventWithOrderTransaction() {
        transactionTemplate.executeWithoutResult(status -> {
            outboxService.save(
                OrderEventType.ORDER_CREATED,
                100L,
                20L,
                Map.of("status", "PENDING_PAYMENT")
            );
            status.setRollbackOnly();
        });

        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void retriesPendingEventAfterTemporaryKafkaFailure() throws Exception {
        OrderOutboxEvent saved = savePendingEvent();
        OrderEventProducer producer = mock(OrderEventProducer.class);
        when(producer.send(any(OrderEventEnvelope.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")))
            .thenReturn(successfulSend());
        OrderOutboxRelay relay = relay(producer, 3);

        executeRelay(relay);
        OrderOutboxEvent failedAttempt = outboxEventRepository.findById(saved.getEventId()).orElseThrow();
        assertThat(failedAttempt.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(failedAttempt.getAttemptCount()).isEqualTo(1);

        Thread.sleep(5);
        executeRelay(relay);
        OrderOutboxEvent published = outboxEventRepository.findById(saved.getEventId()).orElseThrow();

        assertThat(published.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();
        verify(producer, times(2)).send(any(OrderEventEnvelope.class));
    }

    @Test
    void stopsPublishingEventAfterMaximumAttempts() throws Exception {
        OrderOutboxEvent saved = savePendingEvent();
        OrderEventProducer producer = mock(OrderEventProducer.class);
        when(producer.send(any(OrderEventEnvelope.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")));
        OrderOutboxRelay relay = relay(producer, 2);

        executeRelay(relay);
        Thread.sleep(5);
        executeRelay(relay);
        Thread.sleep(5);
        int thirdAttemptCount = executeRelay(relay);

        OrderOutboxEvent abandoned = outboxEventRepository.findById(saved.getEventId()).orElseThrow();
        assertThat(abandoned.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(abandoned.getAttemptCount()).isEqualTo(2);
        assertThat(thirdAttemptCount).isZero();
        verify(producer, times(2)).send(any(OrderEventEnvelope.class));
    }

    @Test
    void stopsCurrentBatchAfterFailureToPreserveEventOrder() throws Exception {
        OrderOutboxEvent first = savePendingEvent();
        Thread.sleep(5);
        OrderOutboxEvent second = transactionTemplate.execute(status -> outboxService.save(
            OrderEventType.PAYMENT_COMPLETED,
            100L,
            20L,
            Map.of("status", "PAID")
        ));
        OrderEventProducer producer = mock(OrderEventProducer.class);
        when(producer.send(any(OrderEventEnvelope.class)))
            .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")));
        OrderOutboxRelay relay = relay(producer, 3);

        int processedCount = executeRelay(relay);

        OrderOutboxEvent failedFirst = outboxEventRepository.findById(first.getEventId()).orElseThrow();
        OrderOutboxEvent untouchedSecond = outboxEventRepository.findById(second.getEventId()).orElseThrow();
        assertThat(processedCount).isEqualTo(1);
        assertThat(failedFirst.getAttemptCount()).isEqualTo(1);
        assertThat(untouchedSecond.getAttemptCount()).isZero();
        verify(producer, times(1)).send(any(OrderEventEnvelope.class));
    }

    private OrderOutboxEvent savePendingEvent() {
        return transactionTemplate.execute(status -> outboxService.save(
            OrderEventType.ORDER_CREATED,
            100L,
            20L,
            Map.of("status", "PENDING_PAYMENT")
        ));
    }

    private OrderOutboxRelay relay(OrderEventProducer producer, int maxAttempts) {
        return new OrderOutboxRelay(
            outboxEventRepository,
            producer,
            new OrderOutboxProperties(
                10,
                maxAttempts,
                Duration.ofMillis(1),
                Duration.ofSeconds(1),
                Duration.ofDays(7)
            ),
            objectMapper,
            new KafkaMessagingMetrics(new SimpleMeterRegistry())
        );
    }

    private int executeRelay(OrderOutboxRelay relay) {
        return transactionTemplate.execute(status -> relay.publishPending());
    }

    private CompletableFuture<SendResult<String, OrderEventEnvelope>> successfulSend() {
        return CompletableFuture.completedFuture(null);
    }
}
