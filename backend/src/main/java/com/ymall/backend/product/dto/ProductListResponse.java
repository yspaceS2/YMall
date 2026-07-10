package com.ymall.backend.product.dto;

import java.math.BigDecimal;

import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;

public record ProductListResponse(
    Long productId,
    Long categoryId,
    String categoryName,
    String name,
    String brand,
    BigDecimal price,
    BigDecimal discountPercentage,
    BigDecimal rating,
    Integer stock,
    String thumbnailUrl,
    ProductStatus status
) {

    public static ProductListResponse from(Product product) {
        return new ProductListResponse(
            product.getId(),
            product.getCategory().getId(),
            product.getCategory().getName(),
            product.getName(),
            product.getBrand(),
            product.getPrice(),
            product.getDiscountPercentage(),
            product.getRating(),
            product.getStock(),
            product.getThumbnailUrl(),
            product.getStatus()
        );
    }
}
