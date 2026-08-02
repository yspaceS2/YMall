package com.ymall.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminDashboardStatisticsResponse(
    DashboardPeriodResponse period,
    BigDecimal netTransactionAmount,
    long orderCount,
    long salesQuantity,
    List<DashboardTrendPointResponse> transactionTrend,
    List<RegistrationTrendPoint> registrationTrend,
    List<CategorySales> categorySales,
    List<DashboardTopProductResponse> topProducts,
    PendingTaskSummary pendingTasks,
    OffsetDateTime generatedAt
) {
    public record RegistrationTrendPoint(
        LocalDate date,
        long members,
        long sellers
    ) {
    }

    public record CategorySales(
        Long categoryId,
        String categoryName,
        BigDecimal netSalesAmount,
        long salesQuantity
    ) {
    }

    public record PendingTaskSummary(
        long products,
        long sellers,
        long refunds,
        long returns,
        long settlements
    ) {
    }
}
