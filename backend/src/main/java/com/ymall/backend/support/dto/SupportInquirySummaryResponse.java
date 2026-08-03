package com.ymall.backend.support.dto;

import java.time.LocalDateTime;

import com.ymall.backend.support.entity.SupportInquiry;
import com.ymall.backend.support.entity.SupportInquiryCategory;
import com.ymall.backend.support.entity.SupportInquiryStatus;
import com.ymall.backend.support.entity.SupportRequesterType;

public record SupportInquirySummaryResponse(
    Long inquiryId,
    SupportRequesterType requesterType,
    String requesterName,
    SupportInquiryCategory category,
    String title,
    SupportInquiryStatus status,
    String assignedAdminName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime closedAt
) {

    public static SupportInquirySummaryResponse from(SupportInquiry inquiry) {
        return new SupportInquirySummaryResponse(
            inquiry.getId(),
            inquiry.getRequesterType(),
            inquiry.getMember().getName(),
            inquiry.getCategory(),
            inquiry.getTitle(),
            inquiry.getStatus(),
            inquiry.getAssignedAdmin() == null ? null : inquiry.getAssignedAdmin().getName(),
            inquiry.getCreatedAt(),
            inquiry.getUpdatedAt(),
            inquiry.getClosedAt()
        );
    }
}
