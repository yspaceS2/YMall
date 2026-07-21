package com.ymall.backend.global.messaging.outbox;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.global.messaging.OrderEventProducer;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"ymall.kafka.enabled", "ymall.kafka.outbox.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class OrderOutboxRelay {

    private final OrderOutboxEventRepository outboxEventRepository;
    private final OrderEventProducer orderEventProducer;
    private final OrderOutboxProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional
    @Scheduled(fixedDelayString = "${ymall.kafka.outbox.poll-interval:1s}")
    public int publishPending() {
        Instant now = Instant.now();
        List<OrderOutboxEvent> events = outboxEventRepository.findPublishable(
            List.of(OutboxEventStatus.PENDING),
            now,
            PageRequest.of(0, properties.batchSize())
        );
        int processedCount = 0;
        for (OrderOutboxEvent event : events) {
            processedCount++;
            if (!publish(event, now)) {
                break;
            }
        }
        return processedCount;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${ymall.kafka.outbox.cleanup-interval:1h}")
    public int cleanupPublished() {
        Instant cutoff = Instant.now().minus(properties.publishedRetention());
        return outboxEventRepository.deletePublishedBefore(OutboxEventStatus.PUBLISHED, cutoff);
    }

    private boolean publish(OrderOutboxEvent event, Instant now) {
        try {
            orderEventProducer.send(event.toEnvelope(objectMapper))
                .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            event.markPublished(now);
            log.debug("Published order outbox event: eventId={}", event.getEventId());
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            event.markFailed(exception.getMessage(), now.plus(properties.retryDelay()), properties.maxAttempts());
            logFailure(event, exception);
            return false;
        } catch (Exception exception) {
            event.markFailed(exception.getMessage(), now.plus(properties.retryDelay()), properties.maxAttempts());
            logFailure(event, exception);
            return true;
        }
    }

    private void logFailure(OrderOutboxEvent event, Exception exception) {
        if (event.getStatus() == OutboxEventStatus.FAILED) {
            log.error(
                "Order outbox event reached maximum publish attempts: eventId={}, attempts={}",
                event.getEventId(),
                event.getAttemptCount(),
                exception
            );
            return;
        }
        log.warn(
            "Order outbox event publish failed and will be retried: eventId={}, attempts={}",
            event.getEventId(),
            event.getAttemptCount(),
            exception
        );
    }
}
