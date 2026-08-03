package com.ymall.backend.support.dto;

import java.time.LocalDateTime;

import com.ymall.backend.support.entity.SupportChatInitiator;
import com.ymall.backend.support.entity.SupportChatSession;
import com.ymall.backend.support.entity.SupportChatStatus;

public record SupportChatSessionResponse(
    Long sessionId,
    Long adminId,
    String adminName,
    SupportChatInitiator initiatedBy,
    SupportChatStatus status,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    LocalDateTime expiresAt
) {

    public static SupportChatSessionResponse from(SupportChatSession session) {
        if (session == null) {
            return null;
        }
        return new SupportChatSessionResponse(
            session.getId(),
            session.getAdmin() == null ? null : session.getAdmin().getId(),
            session.getAdmin() == null ? null : session.getAdmin().getName(),
            session.getInitiatedBy(),
            session.getStatus(),
            session.getStartedAt(),
            session.getEndedAt(),
            session.getExpiresAt()
        );
    }
}
