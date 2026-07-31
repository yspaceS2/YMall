package com.ymall.backend.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ymall.backend.admin.dto.AdminProductChangeStatusUpdateRequest;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductRevision;
import com.ymall.backend.product.entity.ProductRevisionDetailImage;
import com.ymall.backend.product.entity.ProductRevisionImage;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRevisionRepository;

@ExtendWith(MockitoExtension.class)
class ProductChangeReviewServiceTest {

    @Mock
    private ProductRevisionRepository repository;

    @Mock
    private ProductCacheInvalidator cacheInvalidator;

    @InjectMocks
    private ProductChangeReviewService service;

    @Test
    void approvalAppliesProposedContentAndInvalidatesPublicCache() {
        Category currentCategory = new Category("패션", "fashion");
        Category proposedCategory = new Category("상의", "tops");
        Product product = new Product(
            currentCategory,
            "기존 상품",
            "기존 설명",
            "YMall",
            BigDecimal.valueOf(10_000),
            BigDecimal.ZERO,
            null,
            10,
            "old-thumbnail",
            ProductStatus.APPROVED
        );
        ReflectionTestUtils.setField(product, "id", 1L);
        ProductRevision revision = new ProductRevision(
            product,
            proposedCategory,
            "변경 상품",
            "변경 설명",
            "New Brand",
            "new-thumbnail"
        );
        revision.replaceImages(List.of(
            new ProductRevisionImage("original", "image", 0)
        ));
        revision.replaceDetailImages(List.of(
            new ProductRevisionDetailImage("detail-original", "detail-image", 0)
        ));
        ReflectionTestUtils.setField(revision, "id", 100L);
        ReflectionTestUtils.setField(revision, "createdAt", LocalDateTime.now());
        given(repository.findByIdForReview(100L)).willReturn(Optional.of(revision));

        service.review(
            100L,
            new AdminProductChangeStatusUpdateRequest(ProductStatus.APPROVED, null)
        );

        assertThat(revision.getStatus()).isEqualTo(ProductStatus.APPROVED);
        assertThat(product.getName()).isEqualTo("변경 상품");
        assertThat(product.getPrice()).isEqualByComparingTo("10000");
        assertThat(product.getImages()).hasSize(1);
        assertThat(product.getDetailImages()).hasSize(1);
        then(cacheInvalidator).should().evictDetail(1L);
    }
}
