package com.ymall.backend.product.dto;

public record ProductDetailImageResponse(
    Long detailImageId,
    String originalUrl,
    String imageUrl,
    Integer sortOrder
) {
}
