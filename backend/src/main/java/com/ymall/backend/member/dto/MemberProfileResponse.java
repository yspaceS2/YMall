package com.ymall.backend.member.dto;

import java.time.LocalDateTime;

import com.ymall.backend.member.entity.MemberRole;

public record MemberProfileResponse(
    Long memberId,
    String email,
    String name,
    String phone,
    boolean hasPassword,
    MemberRole role,
    LocalDateTime createdAt
) {
}
