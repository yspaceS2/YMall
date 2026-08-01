package com.ymall.backend.product.search;

public record ProductSearchMatch(
    Long productId,
    String name,
    String thumbnailUrl,
    ProductSearchMatchType matchType,
    double similarity,
    long salesQuantity
) {
}
