package com.ymall.backend.dashboard.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class DashboardStatisticsQueryRows {

    private DashboardStatisticsQueryRows() {
    }

    public record TrendRow(LocalDate bucket, BigDecimal netSalesAmount, long orderCount, long salesQuantity) {
    }

    public record StatusCountRow(String status, long count) {
    }

    public record TopProductRow(Long productId, String productName, long salesQuantity, BigDecimal netSalesAmount) {
    }

    public record SettlementRow(BigDecimal availableAmount, BigDecimal processingAmount, BigDecimal completedAmount) {
    }

    public record SellerPendingRow(long orders, long returns, long questions) {
    }

    public record RegistrationRow(LocalDate bucket, long members, long sellers) {
    }

    public record CategorySalesRow(Long categoryId, String categoryName, BigDecimal netSalesAmount, long salesQuantity) {
    }

    public record AdminPendingRow(long products, long sellers, long refunds, long returns, long settlements, long support) {
    }
}
