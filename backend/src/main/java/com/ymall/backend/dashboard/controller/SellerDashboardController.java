package com.ymall.backend.dashboard.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.dashboard.dto.SellerDashboardStatisticsResponse;
import com.ymall.backend.dashboard.service.DashboardStatisticsService;
import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;

@RestController
@RequestMapping("/api/seller/dashboard")
@RequiredArgsConstructor
public class SellerDashboardController {

    private final DashboardStatisticsService dashboardStatisticsService;

    @GetMapping("/statistics")
    public ApiResponse<SellerDashboardStatisticsResponse> getStatistics(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "30d") String period
    ) {
        return ApiResponse.success(
            dashboardStatisticsService.getSellerStatistics(principal.memberId(), period)
        );
    }
}
