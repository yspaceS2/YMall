package com.ymall.backend.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.dashboard.dto.AdminDashboardStatisticsResponse;
import com.ymall.backend.dashboard.service.DashboardStatisticsService;
import com.ymall.backend.global.common.ApiResponse;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardStatisticsService dashboardStatisticsService;

    @GetMapping("/statistics")
    public ApiResponse<AdminDashboardStatisticsResponse> getStatistics(
        @RequestParam(defaultValue = "30d") String period
    ) {
        return ApiResponse.success(dashboardStatisticsService.getAdminStatistics(period));
    }
}
