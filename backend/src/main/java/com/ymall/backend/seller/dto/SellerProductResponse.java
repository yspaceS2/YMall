package com.ymall.backend.seller.dto;

import java.math.BigDecimal;

import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;

public record SellerProductResponse(
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
    ProductStatus status,
    String rejectionReason
) {

    public static SellerProductResponse from(Product product) {
        return new SellerProductResponse(
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
            product.getStatus(),
            product.getRejectionReason()
        );
    }
}
