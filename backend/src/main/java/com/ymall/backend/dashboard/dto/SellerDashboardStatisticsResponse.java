package com.ymall.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SellerDashboardStatisticsResponse(
    DashboardPeriodResponse period,
    BigDecimal netSalesAmount,
    long orderCount,
    long salesQuantity,
    List<DashboardTrendPointResponse> trend,
    List<DashboardStatusCountResponse> orderStatusCounts,
    List<DashboardTopProductResponse> topProducts,
    SettlementSummary settlement,
    PendingTaskSummary pendingTasks,
    OffsetDateTime generatedAt
) {
    public record SettlementSummary(
        BigDecimal availableAmount,
        BigDecimal processingAmount,
        BigDecimal completedAmount
    ) {
    }

    public record PendingTaskSummary(
        long orders,
        long returns,
        long questions
    ) {
    }
}
