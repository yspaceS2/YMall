package com.ymall.backend.support.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportChatExpirationScheduler {

    private final SupportService supportService;

    @Scheduled(fixedDelayString = "${ymall.support.chat-expiration-poll-interval:1m}")
    public void expireWaitingSessions() {
        supportService.expireWaitingSessions();
    }
}
