package com.ymall.backend.product.dto;

import com.ymall.backend.product.entity.Category;

public record CategoryResponse(
    Long categoryId,
    String name,
    String slug,
    Long parentId,
    int depth,
    int displayOrder
) {

    public CategoryResponse(Long categoryId, String name, String slug) {
        this(categoryId, name, slug, null, 1, 0);
    }

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getParent() == null ? null : category.getParent().getId(),
            category.getDepth(),
            category.getDisplayOrder()
        );
    }
}
