package com.ymall.backend.admin.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ymall.backend.admin.dto.AdminAuthorizationResponse;
import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;

@RestController
@RequestMapping("/api/admin/authorization")
public class AdminAuthorizationController {

    @GetMapping
    public ApiResponse<AdminAuthorizationResponse> getAuthorization(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(AdminAuthorizationResponse.from(principal));
    }
}
