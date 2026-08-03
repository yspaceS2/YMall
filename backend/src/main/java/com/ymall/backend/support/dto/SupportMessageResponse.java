package com.ymall.backend.support.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.support.entity.SupportMessage;
import com.ymall.backend.support.entity.SupportMessageType;

public record SupportMessageResponse(
    Long messageId,
    Long authorId,
    String authorName,
    MemberRole authorRole,
    SupportMessageType type,
    String content,
    List<SupportAttachmentResponse> attachments,
    UUID clientMessageId,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {

    public static SupportMessageResponse from(SupportMessage message) {
        return new SupportMessageResponse(
            message.getId(),
            message.getAuthor().getId(),
            message.getAuthor().getName(),
            message.getAuthorRole(),
            message.getType(),
            message.getContent(),
            message.getAttachments().stream()
                .map(SupportAttachmentResponse::from)
                .toList(),
            message.getClientMessageId(),
            message.getReadAt(),
            message.getCreatedAt()
        );
    }
}
