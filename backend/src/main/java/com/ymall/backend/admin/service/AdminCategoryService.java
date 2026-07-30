package com.ymall.backend.admin.service;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.admin.dto.AdminCategoryRequest;
import com.ymall.backend.admin.dto.AdminCategoryResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCategoryService {

    private static final int MAX_DEPTH = 3;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public List<AdminCategoryResponse> getCategories(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<Category> categories = normalizedKeyword.isEmpty()
            ? categoryRepository.findAllByOrderByDepthAscDisplayOrderAscNameAsc()
            : categoryRepository.search(normalizedKeyword);
        return categories.stream().map(this::toResponse).toList();
    }

    public AdminCategoryResponse getCategory(Long categoryId) {
        return toResponse(getCategoryEntity(categoryId));
    }

    @Transactional
    public AdminCategoryResponse createCategory(AdminCategoryRequest request) {
        validateSlug(request.slug(), null);
        Category parent = getParent(request.parentId(), null);
        int depth = parent == null ? 1 : parent.getDepth() + 1;
        validateDepth(depth);
        Category category = new Category(
            request.name().trim(),
            request.slug().trim(),
            parent,
            depth,
            request.displayOrder(),
            request.active()
        );
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public AdminCategoryResponse updateCategory(
        Long categoryId,
        AdminCategoryRequest request
    ) {
        Category category = getCategoryEntity(categoryId);
        validateSlug(request.slug(), categoryId);
        Category parent = getParent(request.parentId(), category);
        int newDepth = parent == null ? 1 : parent.getDepth() + 1;
        int depthOffset = newDepth - category.getDepth();
        List<Category> descendants = collectDescendants(categoryId);
        int deepestDepth = descendants.stream()
            .mapToInt(Category::getDepth)
            .max()
            .orElse(category.getDepth());
        validateDepth(deepestDepth + depthOffset);

        category.update(
            request.name().trim(),
            request.slug().trim(),
            parent,
            newDepth,
            request.displayOrder(),
            request.active()
        );
        descendants.forEach(descendant ->
            descendant.changeDepth(descendant.getDepth() + depthOffset)
        );
        return toResponse(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = getCategoryEntity(categoryId);
        if (categoryRepository.existsByParentId(categoryId)
            || productRepository.existsByCategoryId(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_DELETE_NOT_ALLOWED);
        }
        categoryRepository.delete(category);
    }

    private Category getCategoryEntity(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private Category getParent(Long parentId, Category category) {
        if (parentId == null) {
            return null;
        }
        if (category != null && category.getId().equals(parentId)) {
            throw new BusinessException(ErrorCode.CATEGORY_PARENT_INVALID);
        }
        Category parent = getCategoryEntity(parentId);
        Category cursor = parent;
        while (cursor != null) {
            if (category != null && cursor.getId().equals(category.getId())) {
                throw new BusinessException(ErrorCode.CATEGORY_PARENT_INVALID);
            }
            cursor = cursor.getParent();
        }
        return parent;
    }

    private List<Category> collectDescendants(Long parentId) {
        List<Category> children =
            categoryRepository.findByParentIdOrderByDisplayOrderAscNameAsc(parentId);
        return children.stream()
            .flatMap(child -> Stream.concat(
                Stream.of(child),
                collectDescendants(child.getId()).stream()
            ))
            .toList();
    }

    private void validateSlug(String slug, Long categoryId) {
        boolean duplicated = categoryId == null
            ? categoryRepository.existsBySlugIgnoreCase(slug)
            : categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, categoryId);
        if (duplicated) {
            throw new BusinessException(ErrorCode.CATEGORY_SLUG_DUPLICATED);
        }
    }

    private void validateDepth(int depth) {
        if (depth < 1 || depth > MAX_DEPTH) {
            throw new BusinessException(ErrorCode.CATEGORY_DEPTH_EXCEEDED);
        }
    }

    private AdminCategoryResponse toResponse(Category category) {
        return AdminCategoryResponse.from(
            category,
            categoryRepository.existsByParentId(category.getId()),
            productRepository.existsByCategoryId(category.getId())
        );
    }
}
