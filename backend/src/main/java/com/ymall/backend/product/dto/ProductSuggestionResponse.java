package com.ymall.backend.product.dto;

import com.ymall.backend.product.search.ProductSearchMatch;
import com.ymall.backend.product.search.ProductSearchMatchType;

public record ProductSuggestionResponse(
    Long productId,
    String name,
    String thumbnailUrl,
    ProductSearchMatchType matchType
) {

    public static ProductSuggestionResponse from(ProductSearchMatch match) {
        return new ProductSuggestionResponse(
            match.productId(),
            match.name(),
            match.thumbnailUrl(),
            match.matchType()
        );
    }
}
