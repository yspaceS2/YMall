package com.ymall.backend.product.repository;

import java.util.List;
import java.util.Set;

import com.ymall.backend.product.search.ProductSearchMatch;

public interface ProductSuggestionFinder {

    List<ProductSearchMatch> findMatches(
        String normalizedKeyword,
        Set<Long> categoryIds,
        int limit
    );
}
