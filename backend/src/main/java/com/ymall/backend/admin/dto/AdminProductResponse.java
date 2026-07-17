package com.ymall.backend.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ymall.backend.product.entity.ProductStatus;

public record AdminProductResponse(
    Long productId,
    Long sellerProfileId,
    String storeName,
    String categoryName,
    String name,
    String brand,
    BigDecimal price,
    Integer stock,
    String thumbnailUrl,
    ProductStatus status,
    LocalDateTime createdAt
) {
}
