package com.ymall.backend.home.dto;

import java.math.BigDecimal;

public record HomeMerchandisingProductResponse(
    Long productId,
    Long categoryId,
    String categoryName,
    String name,
    String brand,
    BigDecimal price,
    BigDecimal discountPercentage,
    BigDecimal rating,
    String thumbnailUrl,
    long salesQuantity
) {
}
