package com.ymall.backend.product.dto;

public record ProductImageSnapshot(
    String originalUrl,
    String imageUrl,
    Integer sortOrder
) {
}
