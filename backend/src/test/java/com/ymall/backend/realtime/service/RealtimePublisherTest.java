package com.ymall.backend.realtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.ymall.backend.realtime.dto.RealtimeEvent;

class RealtimePublisherTest {

    @Test
    void inquiryTopicPublishesOnlyInvalidationMetadata() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RealtimePublisher publisher = new RealtimePublisher(messagingTemplate);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        publisher.publishInquiry(31L);

        verify(messagingTemplate).convertAndSend(
            eq("/topic/support/inquiries/31"),
            payloadCaptor.capture()
        );
        assertThat(payloadCaptor.getValue())
            .isInstanceOf(RealtimeEvent.class);
        RealtimeEvent event = (RealtimeEvent) payloadCaptor.getValue();
        assertThat(event.type()).isEqualTo("SUPPORT_INQUIRY_CHANGED");
        assertThat(event.resource()).isEqualTo("supportInquiry");
        assertThat(event.resourceId()).isEqualTo(31L);
    }
}
