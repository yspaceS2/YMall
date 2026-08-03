package com.ymall.backend.admin.dto;

import java.util.Set;

import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.admin.entity.AdminPermission;
import com.ymall.backend.global.security.MemberPrincipal;

public record AdminAuthorizationResponse(
    Long memberId,
    AdminGrade adminGrade,
    Set<AdminPermission> permissions
) {
    public static AdminAuthorizationResponse from(MemberPrincipal principal) {
        return new AdminAuthorizationResponse(
            principal.memberId(),
            principal.adminGrade(),
            principal.permissions()
        );
    }
}
