package com.ymall.backend.product.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.repository.CategoryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCategoryPolicy {

    private final CategoryRepository categoryRepository;

    public Category getSelectableCategory(Long categoryId) {
        Category category = getCategory(categoryId);
        if (!isPathActive(category) || categoryRepository.existsByParentId(category.getId())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_SELECTABLE);
        }
        return category;
    }

    public Set<Long> getPublicTreeIds(Long categoryId) {
        Category rootCategory = getCategory(categoryId);
        if (!isPathActive(rootCategory)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return collectActiveTreeIds(rootCategory);
    }

    public Set<Long> getActiveTreeIds(Long rootCategoryId) {
        Category rootCategory = categoryRepository.findById(rootCategoryId)
            .filter(Category::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        return collectActiveTreeIds(rootCategory);
    }

    private Set<Long> collectActiveTreeIds(Category rootCategory) {
        List<Category> activeCategories = categoryRepository.findByActiveTrue(Sort.unsorted());
        Set<Long> categoryIds = new HashSet<>();
        categoryIds.add(rootCategory.getId());

        boolean categoryAdded;
        do {
            categoryAdded = false;
            for (Category category : activeCategories) {
                Category parent = category.getParent();
                if (parent != null
                    && categoryIds.contains(parent.getId())
                    && categoryIds.add(category.getId())) {
                    categoryAdded = true;
                }
            }
        } while (categoryAdded);

        return categoryIds;
    }

    public boolean isPathActive(Category category) {
        Category cursor = category;
        while (cursor != null) {
            if (!cursor.isActive()) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return true;
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
