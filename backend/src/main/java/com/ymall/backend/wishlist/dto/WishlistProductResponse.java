package com.ymall.backend.wishlist.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.wishlist.entity.WishlistItem;

public record WishlistProductResponse(
    Long productId,
    String name,
    String brand,
    BigDecimal price,
    BigDecimal discountPercentage,
    BigDecimal rating,
    Integer stock,
    String thumbnailUrl,
    ProductStatus status,
    LocalDateTime wishedAt
) {

    public static WishlistProductResponse from(WishlistItem item) {
        Product product = item.getProduct();
        return new WishlistProductResponse(
            product.getId(),
            product.getName(),
            product.getBrand(),
            product.getPrice(),
            product.getDiscountPercentage(),
            product.getRating(),
            product.getStock(),
            product.getThumbnailUrl(),
            product.getStatus(),
            item.getCreatedAt()
        );
    }
}
