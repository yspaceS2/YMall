package com.ymall.backend.product.dto;

import com.ymall.backend.product.entity.Category;

public record CategoryResponse(
    Long categoryId,
    String name,
    String slug
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getSlug()
        );
    }
}
