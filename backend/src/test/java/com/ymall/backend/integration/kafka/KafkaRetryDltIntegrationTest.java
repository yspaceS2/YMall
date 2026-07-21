package com.ymall.backend.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.global.messaging.OrderEventProducer;
import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.notification.repository.NotificationRepository;
import com.ymall.backend.notification.repository.ProcessedOrderEventRepository;
import com.ymall.backend.notification.service.OrderNotificationEventProcessor;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:ymall-kafka-retry-dlt-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "ymall.kafka.enabled=true",
    "ymall.kafka.notification-consumer.enabled=true",
    "ymall.kafka.notification-consumer.group-id=ymall-retry-dlt-integration-test",
    "ymall.kafka.consumer-retry.max-retries=2",
    "ymall.kafka.consumer-retry.retry-delay=10ms"
})
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 3,
    topics = {"ymall.order.events.v1", "ymall.order.events.v1.DLT"},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class KafkaRetryDltIntegrationTest {

    @Autowired private OrderEventProducer producer;
    @Autowired private MemberRepository memberRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ProcessedOrderEventRepository processedEventRepository;
    @Autowired private DltEventListener dltEventListener;

    @MockitoSpyBean
    private OrderNotificationEventProcessor eventProcessor;

    @AfterEach
    void tearDown() {
        reset(eventProcessor);
        dltEventListener.events.clear();
    }

    @Test
    void retriesTemporaryFailureAndEventuallyProcessesEvent() throws Exception {
        Member member = saveMember("retry-success@example.com");
        OrderEventEnvelope event = event(801L, member.getId());
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.getAndIncrement() == 0) {
                throw new TransientDataAccessResourceException("temporary database failure");
            }
            return invocation.callRealMethod();
        }).when(eventProcessor).process(argThat(candidate ->
            candidate.eventId().equals(event.eventId())
        ));

        producer.send(event).get(10, TimeUnit.SECONDS);
        awaitProcessed(event, Duration.ofSeconds(10));

        verify(eventProcessor, times(2)).process(argThat(candidate ->
            candidate.eventId().equals(event.eventId())
        ));
        assertThat(notificationRepository.existsBySourceEventId(event.eventId())).isTrue();
    }

    @Test
    void sendsEventToDltAfterTemporaryFailureRetriesAreExhausted() throws Exception {
        Member member = saveMember("retry-exhausted@example.com");
        OrderEventEnvelope event = event(802L, member.getId());
        doThrow(new TransientDataAccessResourceException("database unavailable"))
            .when(eventProcessor)
            .process(argThat(candidate -> candidate.eventId().equals(event.eventId())));

        producer.send(event).get(10, TimeUnit.SECONDS);
        OrderEventEnvelope deadLetter = dltEventListener.poll(event, 10, TimeUnit.SECONDS);

        assertThat(deadLetter).isEqualTo(event);
        verify(eventProcessor, times(3)).process(argThat(candidate ->
            candidate.eventId().equals(event.eventId())
        ));
        assertThat(processedEventRepository.existsByEventId(event.eventId())).isFalse();
        assertThat(notificationRepository.existsBySourceEventId(event.eventId())).isFalse();
    }

    @Test
    void sendsNonRetryableBusinessFailureDirectlyToDlt() throws Exception {
        Member member = saveMember("non-retryable@example.com");
        OrderEventEnvelope event = event(803L, member.getId());
        doThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND))
            .when(eventProcessor)
            .process(argThat(candidate -> candidate.eventId().equals(event.eventId())));

        producer.send(event).get(10, TimeUnit.SECONDS);
        OrderEventEnvelope deadLetter = dltEventListener.poll(event, 10, TimeUnit.SECONDS);

        assertThat(deadLetter).isEqualTo(event);
        verify(eventProcessor, times(1)).process(argThat(candidate ->
            candidate.eventId().equals(event.eventId())
        ));
    }

    private Member saveMember(String email) {
        return memberRepository.save(new Member(
            email, "password", "Kafka Retry Tester", MemberRole.ROLE_USER
        ));
    }

    private OrderEventEnvelope event(Long orderId, Long memberId) {
        return OrderEventEnvelope.create(
            OrderEventType.ORDER_CREATED, orderId, memberId, Map.of()
        );
    }

    private void awaitProcessed(OrderEventEnvelope event, Duration timeout)
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

    @TestConfiguration
    static class DltListenerConfiguration {

        @Bean
        DltEventListener dltEventListener() {
            return new DltEventListener();
        }
    }

    static class DltEventListener {

        private final BlockingQueue<OrderEventEnvelope> events = new LinkedBlockingQueue<>();

        @KafkaListener(
            topics = "${ymall.kafka.order-events.name}.DLT",
            groupId = "ymall-dlt-integration-test"
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
