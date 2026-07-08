package com.ymall.backend.product.dto;

import com.ymall.backend.product.entity.ProductImage;

public record ProductImageResponse(
    Long imageId,
    String originalUrl,
    String imageUrl,
    Integer sortOrder
) {

    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(
            image.getId(),
            image.getOriginalUrl(),
            image.getImageUrl(),
            image.getSortOrder()
        );
    }
}
