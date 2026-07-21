package com.ymall.backend.integration.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
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
import com.ymall.backend.notification.repository.NotificationRepository;

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

    @Test
    void mapsEveryOrderEventAndIgnoresDuplicateEventId() throws Exception {
        Member member = memberRepository.save(new Member(
            "kafka-notification@example.com",
            "password",
            "Kafka Notification Tester",
            MemberRole.ROLE_USER
        ));
        Long orderId = 701L;
        OrderEventEnvelope[] events = Arrays.stream(OrderEventType.values())
            .map(type -> OrderEventEnvelope.create(type, orderId, member.getId(), Map.of()))
            .toArray(OrderEventEnvelope[]::new);

        for (OrderEventEnvelope event : events) {
            producer.send(event).get(10, TimeUnit.SECONDS);
        }
        producer.send(events[0]).get(10, TimeUnit.SECONDS);

        awaitNotificationCount(member.getId(), events.length, Duration.ofSeconds(10));

        var notifications = notificationRepository.findByMemberId(
            member.getId(), PageRequest.of(0, 20)
        ).getContent();
        assertThat(notifications).hasSize(events.length);
        assertThat(notifications)
            .extracting(notification -> notification.getType())
            .containsExactlyInAnyOrder(
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
