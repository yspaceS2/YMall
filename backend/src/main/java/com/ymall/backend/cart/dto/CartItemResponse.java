package com.ymall.backend.cart.dto;

import java.math.BigDecimal;

import com.ymall.backend.product.entity.ProductStatus;

public record CartItemResponse(
    Long cartItemId,
    Long productId,
    String productName,
    String thumbnailUrl,
    BigDecimal price,
    BigDecimal discountPercentage,
    Integer stock,
    ProductStatus productStatus,
    Integer quantity
) {
}
