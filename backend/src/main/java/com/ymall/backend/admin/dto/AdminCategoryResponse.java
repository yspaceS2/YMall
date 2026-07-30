package com.ymall.backend.admin.dto;

import java.time.LocalDateTime;

import com.ymall.backend.product.entity.Category;

public record AdminCategoryResponse(
    Long categoryId,
    String name,
    String slug,
    Long parentId,
    String parentName,
    int depth,
    int displayOrder,
    boolean active,
    boolean hasChildren,
    boolean hasProducts,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static AdminCategoryResponse from(
        Category category,
        boolean hasChildren,
        boolean hasProducts
    ) {
        Category parent = category.getParent();
        return new AdminCategoryResponse(
            category.getId(),
            category.getName(),
            category.getSlug(),
            parent == null ? null : parent.getId(),
            parent == null ? null : parent.getName(),
            category.getDepth(),
            category.getDisplayOrder(),
            category.isActive(),
            hasChildren,
            hasProducts,
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}
