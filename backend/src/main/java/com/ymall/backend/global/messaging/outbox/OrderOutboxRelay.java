package com.ymall.backend.global.messaging.outbox;

import java.time.Clock;
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

import com.ymall.backend.global.messaging.KafkaMessagingMetrics;
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
    private final KafkaMessagingMetrics metrics;
    private final Clock clock;

    /**
     * 대기 이벤트 한 묶음을 순서대로 발행하며 첫 실패에서 처리를 중단한다.
     *
     * <p>첫 실패에서 중단하여 같은 폴링 묶음의 후속 이벤트를 곧바로 전송하지 않는다. 다만 실패
     * 이벤트의 재시도 대기 중 다음 폴링이 실행될 수 있으므로 전체 이벤트의 엄격한 순서를 보장하지는
     * 않는다. Kafka가 전송을 확인할 때까지 DB 트랜잭션을 유지하여 Broker가 받기 전에 발행 완료로
     * 기록되지 않게 한다. 전송은 at-least-once이므로 Consumer는 이벤트 ID로 중복을 제거하고
     * 상태 역행을 방어해야 한다.</p>
     *
     * @return 마지막 실패 시도를 포함해 이번 폴링에서 발행을 시도한 이벤트 수
     */
    @Transactional
    @Scheduled(fixedDelayString = "${ymall.kafka.outbox.poll-interval:1s}")
    public int publishPending() {
        Instant now = clock.instant();
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
        Instant cutoff = clock.instant().minus(properties.publishedRetention());
        return outboxEventRepository.deletePublishedBefore(OutboxEventStatus.PUBLISHED, cutoff);
    }

    private boolean publish(OrderOutboxEvent event, Instant now) {
        try {
            orderEventProducer.send(event.toEnvelope(objectMapper))
                .get(properties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            event.markPublished(now);
            metrics.recordOutboxPublished();
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
            return false;
        }
    }

    private void logFailure(OrderOutboxEvent event, Exception exception) {
        metrics.recordOutboxFailure(event.getStatus() == OutboxEventStatus.FAILED);
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
            "Order outbox event publish failed and will be retried: eventId={}, attempts={}, error={}",
            event.getEventId(),
            event.getAttemptCount(),
            exception.getClass().getSimpleName()
        );
    }
}
