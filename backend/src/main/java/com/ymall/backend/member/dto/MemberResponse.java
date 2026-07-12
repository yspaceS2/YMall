package com.ymall.backend.member.dto;

import java.time.LocalDateTime;

import com.ymall.backend.member.entity.MemberRole;

public record MemberResponse(
    Long memberId,
    String email,
    String name,
    MemberRole role,
    LocalDateTime createdAt
) {
}
