package com.ymall.backend.dashboard.dto;

import java.math.BigDecimal;

public record DashboardTopProductResponse(
    Long productId,
    String productName,
    long salesQuantity,
    BigDecimal netSalesAmount
) {
}
