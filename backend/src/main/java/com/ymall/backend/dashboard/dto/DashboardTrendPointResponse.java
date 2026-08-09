package com.ymall.backend.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardTrendPointResponse(
    LocalDate date,
    BigDecimal netSalesAmount,
    long orderCount,
    long salesQuantity
) {
}
