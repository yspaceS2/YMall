package com.ymall.backend.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class ProductCategoryPolicyTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductCategoryPolicy productCategoryPolicy;

    @Test
    void returnsSelectableCategoryWhenCategoryAndAncestorsAreActive() {
        Category parent = category(1L, true, null);
        Category category = category(2L, true, parent);
        given(categoryRepository.findById(2L)).willReturn(Optional.of(category));
        given(categoryRepository.existsByParentId(2L)).willReturn(false);

        Category result = productCategoryPolicy.getSelectableCategory(2L);

        assertThat(result).isSameAs(category);
    }

    @Test
    void rejectsSelectableCategoryWhenAncestorIsInactive() {
        Category parent = category(1L, false, null);
        Category category = category(2L, true, parent);
        given(categoryRepository.findById(2L)).willReturn(Optional.of(category));

        assertThatThrownBy(() -> productCategoryPolicy.getSelectableCategory(2L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CATEGORY_NOT_SELECTABLE);
    }

    @Test
    void rejectsSelectableCategoryWhenCategoryHasChildren() {
        Category category = category(1L, true, null);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(categoryRepository.existsByParentId(1L)).willReturn(true);

        assertThatThrownBy(() -> productCategoryPolicy.getSelectableCategory(1L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CATEGORY_NOT_SELECTABLE);
    }

    @Test
    void rejectsPublicCategoryWhenAncestorIsInactive() {
        Category parent = category(1L, false, null);
        Category category = category(2L, true, parent);
        given(categoryRepository.findById(2L)).willReturn(Optional.of(category));

        assertThatThrownBy(() -> productCategoryPolicy.getPublicTreeIds(2L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void collectsActiveDescendantsRegardlessOfRepositoryOrder() {
        Category inactiveAncestor = category(9L, false, null);
        Category root = category(1L, true, inactiveAncestor);
        Category child = category(2L, true, root);
        Category grandchild = category(3L, true, child);
        Category unrelated = category(4L, true, null);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(root));
        given(categoryRepository.findByActiveTrue(any()))
            .willReturn(List.of(grandchild, unrelated, child, root));

        assertThat(productCategoryPolicy.getActiveTreeIds(1L))
            .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    private Category category(Long id, boolean active, Category parent) {
        Category category = mock(Category.class);
        lenient().when(category.getId()).thenReturn(id);
        lenient().when(category.isActive()).thenReturn(active);
        lenient().when(category.getParent()).thenReturn(parent);
        return category;
    }
}
