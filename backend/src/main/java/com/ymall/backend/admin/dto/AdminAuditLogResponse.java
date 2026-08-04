package com.ymall.backend.admin.dto;

import java.time.LocalDateTime;

import com.ymall.backend.admin.entity.AdminAuditAction;
import com.ymall.backend.admin.entity.AdminGrade;

public record AdminAuditLogResponse(
    Long auditLogId,
    Long actorMemberId,
    String actorName,
    AdminGrade actorGrade,
    AdminAuditAction action,
    String beforeValue,
    String afterValue,
    String reason,
    LocalDateTime createdAt
) {
}
