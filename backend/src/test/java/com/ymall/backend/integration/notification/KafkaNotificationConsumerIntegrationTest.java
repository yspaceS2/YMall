package com.ymall.backend.integration.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.global.messaging.OrderEventProducer;
import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.notification.entity.NotificationType;
import com.ymall.backend.notification.entity.OrderEventProcessingResult;
import com.ymall.backend.notification.repository.NotificationRepository;
import com.ymall.backend.notification.repository.ProcessedOrderEventRepository;
import com.ymall.backend.notification.service.OrderNotificationEventProcessor;

@SpringBootTest(properties = {
    "ymall.kafka.enabled=true",
    "ymall.kafka.notification-consumer.enabled=true",
    "ymall.kafka.notification-consumer.group-id=ymall-notification-integration-test"
})
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 3,
    topics = "ymall.order.events.v1",
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class KafkaNotificationConsumerIntegrationTest {

    @Autowired private OrderEventProducer producer;
    @Autowired private MemberRepository memberRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ProcessedOrderEventRepository processedEventRepository;
    @Autowired private OrderNotificationEventProcessor eventProcessor;

    @Test
    void mapsEveryOrderEventAndIgnoresDuplicateEventId() throws Exception {
        Member member = memberRepository.save(new Member(
            "kafka-notification@example.com",
            "password",
            "Kafka Notification Tester",
            MemberRole.ROLE_USER
        ));
        Long fulfilledOrderId = 701L;
        Long canceledOrderId = 702L;
        List<OrderEventEnvelope> events = List.of(
            event(OrderEventType.ORDER_CREATED, fulfilledOrderId, member.getId()),
            event(OrderEventType.PAYMENT_FAILED, fulfilledOrderId, member.getId()),
            event(OrderEventType.PAYMENT_COMPLETED, fulfilledOrderId, member.getId()),
            event(OrderEventType.ORDER_PREPARING, fulfilledOrderId, member.getId()),
            event(OrderEventType.ORDER_SHIPPED, fulfilledOrderId, member.getId()),
            event(OrderEventType.ORDER_DELIVERED, fulfilledOrderId, member.getId()),
            event(OrderEventType.ORDER_CREATED, canceledOrderId, member.getId()),
            event(OrderEventType.ORDER_CANCELED, canceledOrderId, member.getId())
        );
        OrderEventEnvelope reversedEvent = event(
            OrderEventType.ORDER_PREPARING, fulfilledOrderId, member.getId()
        );

        for (OrderEventEnvelope event : events) {
            producer.send(event).get(10, TimeUnit.SECONDS);
        }
        producer.send(events.get(0)).get(10, TimeUnit.SECONDS);
        producer.send(reversedEvent).get(10, TimeUnit.SECONDS);

        awaitProcessing(reversedEvent, Duration.ofSeconds(10));
        awaitNotificationCount(member.getId(), events.size(), Duration.ofSeconds(10));

        var notifications = notificationRepository.findByMemberId(
            member.getId(), PageRequest.of(0, 20)
        ).getContent();
        assertThat(notifications).hasSize(events.size());
        assertThat(notifications)
            .extracting(notification -> notification.getType())
            .containsExactlyInAnyOrder(
                NotificationType.ORDER_CREATED,
                NotificationType.ORDER_CREATED,
                NotificationType.PAYMENT_COMPLETED,
                NotificationType.PAYMENT_FAILED,
                NotificationType.ORDER_CANCELED,
                NotificationType.ORDER_PREPARING,
                NotificationType.ORDER_SHIPPED,
                NotificationType.ORDER_DELIVERED
            );
        assertThat(notifications)
            .extracting(notification -> notification.getSourceEventId())
            .doesNotHaveDuplicates();
        assertThat(processedEventRepository.findByEventId(reversedEvent.eventId()))
            .get()
            .extracting(processedEvent -> processedEvent.getResult())
            .isEqualTo(OrderEventProcessingResult.REJECTED);
        assertThat(processedEventRepository.findAll().stream()
            .filter(processed -> events.stream()
                .anyMatch(event -> event.eventId().equals(processed.getEventId())))
            .count()).isEqualTo(events.size());
    }

    @Test
    void rollsBackProcessingHistoryWhenNotificationCreationFails() {
        OrderEventEnvelope event = event(OrderEventType.ORDER_CREATED, 703L, Long.MAX_VALUE);

        assertThatThrownBy(() -> eventProcessor.process(event))
            .isInstanceOf(RuntimeException.class);
        assertThat(processedEventRepository.existsByEventId(event.eventId())).isFalse();
        assertThat(notificationRepository.existsBySourceEventId(event.eventId())).isFalse();
    }

    private OrderEventEnvelope event(OrderEventType type, Long orderId, Long memberId) {
        return OrderEventEnvelope.create(type, orderId, memberId, Map.of());
    }

    private void awaitProcessing(OrderEventEnvelope event, Duration timeout)
        throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (processedEventRepository.existsByEventId(event.eventId())) {
                return;
            }
            Thread.sleep(100);
        }
        assertThat(processedEventRepository.existsByEventId(event.eventId())).isTrue();
    }

    private void awaitNotificationCount(Long memberId, int expectedCount, Duration timeout)
        throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (notificationRepository.findByMemberId(memberId, PageRequest.of(0, 20))
                .getTotalElements() == expectedCount) {
                return;
            }
            Thread.sleep(100);
        }
        assertThat(notificationRepository.findByMemberId(memberId, PageRequest.of(0, 20))
            .getTotalElements()).isEqualTo(expectedCount);
    }
}
