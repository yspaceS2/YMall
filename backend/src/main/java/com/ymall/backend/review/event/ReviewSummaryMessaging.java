package com.ymall.backend.review.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ymall.backend.review.config.ReviewSummaryProperties;
import com.ymall.backend.review.service.ReviewSummaryService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewSummaryMessaging {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ReviewSummaryService reviewSummaryService;
    private final ReviewSummaryProperties properties;

    @Value("${ymall.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${ymall.kafka.review-summary.name}")
    private String topicName;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ReviewSummaryRefreshEvent event) {
        reviewSummaryService.evict(event.productId());
        if (!kafkaEnabled || !properties.enabled()) {
            return;
        }
        kafkaTemplate.send(topicName, event.productId().toString(), event)
            .whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error(
                        "Review summary refresh event publish failed. productId={}, eventId={}",
                        event.productId(),
                        event.eventId(),
                        exception
                    );
                }
            });
    }

    @KafkaListener(
        topics = "${ymall.kafka.review-summary.name}",
        groupId = "${ymall.kafka.review-summary.group-id}",
        autoStartup = "${ymall.kafka.enabled:true}",
        properties = {
            "spring.json.value.default.type=com.ymall.backend.review.event.ReviewSummaryRefreshEvent",
            "spring.json.trusted.packages=com.ymall.backend.review.event"
        }
    )
    public void consume(ReviewSummaryRefreshEvent event) {
        reviewSummaryService.refresh(event.productId());
    }
}
