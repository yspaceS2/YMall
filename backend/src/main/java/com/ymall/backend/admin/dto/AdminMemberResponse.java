package com.ymall.backend.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.entity.MemberAccessStatus;

public record AdminMemberResponse(
    Long memberId,
    String email,
    String name,
    MemberRole role,
    AdminGrade adminGrade,
    MemberAccessStatus accessStatus,
    LocalDateTime lastLoginAt,
    String restrictionReason,
    LocalDateTime restrictedAt,
    Long restrictedByMemberId,
    long orderCount,
    BigDecimal totalPaidAmount,
    LocalDateTime createdAt
) {
}
