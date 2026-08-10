package com.ymall.backend.home.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HomeMerchandisingRow(
    HomeMerchandisingSection section,
    Long groupCategoryId,
    String groupCategoryName,
    String groupCategorySlug,
    Long productId,
    Long categoryId,
    String categoryName,
    String productName,
    String brand,
    BigDecimal price,
    BigDecimal discountPercentage,
    LocalDate discountStartDate,
    LocalDate discountEndDate,
    BigDecimal rating,
    long reviewCount,
    String thumbnailUrl,
    long salesQuantity
) {
}
