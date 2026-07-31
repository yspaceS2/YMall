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
    boolean freeShipping,
    BigDecimal shippingFee,
    Integer estimatedDeliveryDays,
    Integer stock,
    ProductStatus productStatus,
    Integer quantity
) {
    public CartItemResponse(
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
        this(
            cartItemId,
            productId,
            productName,
            thumbnailUrl,
            price,
            discountPercentage,
            true,
            BigDecimal.ZERO,
            3,
            stock,
            productStatus,
            quantity
        );
    }
}
