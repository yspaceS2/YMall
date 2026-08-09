package com.ymall.backend.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.ymall.backend.product.dto.ProductDetailImageResponse;
import com.ymall.backend.product.dto.ProductImageResponse;
import com.ymall.backend.product.entity.ProductStatus;

public record AdminProductResponse(
    Long productId,
    Long sellerProfileId,
    String storeName,
    String categoryName,
    String name,
    String description,
    String brand,
    BigDecimal price,
    BigDecimal discountPercentage,
    LocalDate discountStartDate,
    LocalDate discountEndDate,
    Integer stock,
    String thumbnailUrl,
    boolean freeShipping,
    BigDecimal shippingFee,
    Integer estimatedDeliveryDays,
    List<ProductImageResponse> images,
    List<ProductDetailImageResponse> detailImages,
    ProductStatus status,
    String rejectionReason,
    LocalDateTime createdAt
) {
}
