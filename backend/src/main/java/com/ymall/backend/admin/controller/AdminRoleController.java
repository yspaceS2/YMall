package com.ymall.backend.admin.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.admin.dto.AdminRoleResponse;
import com.ymall.backend.admin.dto.AdminRoleUpdateRequest;
import com.ymall.backend.admin.service.AdminRoleService;
import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/members")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @PatchMapping("/{memberId}/admin-role")
    public ApiResponse<AdminRoleResponse> changeAdminRole(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long memberId,
        @Valid @RequestBody AdminRoleUpdateRequest request
    ) {
        return ApiResponse.success(
            adminRoleService.changeRole(principal.memberId(), memberId, request),
            "관리자 역할을 변경했습니다."
        );
    }
}
