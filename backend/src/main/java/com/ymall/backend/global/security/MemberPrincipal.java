package com.ymall.backend.global.security;

import com.ymall.backend.member.entity.MemberRole;

public record MemberPrincipal(
    Long memberId,
    String email,
    MemberRole role
) {
}
