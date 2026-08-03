package com.ymall.backend.support.dto;

import com.ymall.backend.support.entity.SupportAttachment;

public record SupportAttachmentResponse(
    Long attachmentId,
    String fileName,
    String contentType,
    long fileSize,
    String downloadUrl
) {

    public static SupportAttachmentResponse from(SupportAttachment attachment) {
        return new SupportAttachmentResponse(
            attachment.getId(),
            attachment.getOriginalFileName(),
            attachment.getContentType(),
            attachment.getFileSize(),
            "/api/support/attachments/" + attachment.getId()
        );
    }
}
