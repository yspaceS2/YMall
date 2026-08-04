package com.ymall.backend.realtime.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ymall.backend.realtime.dto.RealtimeEvent;

@Component
@RequiredArgsConstructor
public class RealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishToMember(Long memberId, RealtimeEvent event) {
        afterCommit(() -> messagingTemplate.convertAndSend(
            "/topic/realtime/members/" + memberId,
            event
        ));
    }

    public void publishToAdmins(RealtimeEvent event) {
        afterCommit(() -> messagingTemplate.convertAndSend("/topic/realtime/admin", event));
    }

    public void publishInquiry(Long inquiryId) {
        RealtimeEvent event = RealtimeEvent.of(
            "SUPPORT_INQUIRY_CHANGED",
            "supportInquiry",
            inquiryId
        );
        afterCommit(() -> messagingTemplate.convertAndSend(
            "/topic/support/inquiries/" + inquiryId,
            event
        ));
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            }
        );
    }
}
