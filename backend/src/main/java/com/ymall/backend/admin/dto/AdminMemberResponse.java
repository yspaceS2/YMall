package com.ymall.backend.admin.dto;

import java.time.LocalDateTime;

import com.ymall.backend.member.entity.MemberRole;

public record AdminMemberResponse(
    Long memberId,
    String email,
    String name,
    MemberRole role,
    LocalDateTime createdAt
) {
}
