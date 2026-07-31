package com.ymall.backend.seller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ymall.backend.product.dto.ProductDetailImageCreateRequest;
import com.ymall.backend.product.dto.ProductDetailResponse;
import com.ymall.backend.product.dto.ProductUpdateRequest;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductDetailImage;
import com.ymall.backend.product.entity.ProductImage;
import com.ymall.backend.product.entity.ProductRevision;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.mapper.ProductMapper;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.repository.ProductRevisionRepository;
import com.ymall.backend.product.service.ProductCacheInvalidator;
import com.ymall.backend.seller.entity.SellerProfile;

@ExtendWith(MockitoExtension.class)
class SellerProductServiceTest {

    @Mock
    private SellerProfileService sellerProfileService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductCacheInvalidator productCacheInvalidator;
    @Mock
    private ProductRevisionRepository productRevisionRepository;
    @Mock
    private SellerProfile sellerProfile;
    @Mock
    private Product product;
    @Mock
    private Category category;
    @Mock
    private ProductDetailResponse response;

    @InjectMocks
    private SellerProductService sellerProductService;

    @Test
    void pendingProductUpdateReplacesDetailImages() {
        ProductUpdateRequest request = request(
            "수정 상품",
            List.of(new ProductDetailImageCreateRequest(
                "detail-original",
                "detail-image",
                0
            ))
        );
        List<ProductImage> images = List.of();
        List<ProductDetailImage> detailImages = List.of(
            new ProductDetailImage("detail-original", "detail-image", 0)
        );
        givenOwnedProduct(ProductStatus.PENDING);
        given(productMapper.toImageEntities(request)).willReturn(images);
        given(productMapper.toDetailImageEntities(request)).willReturn(detailImages);
        given(productMapper.toProductDetailResponse(product)).willReturn(response);

        sellerProductService.updateProduct(1L, 10L, request);

        then(product).should().replaceImages(images);
        then(product).should().replaceDetailImages(detailImages);
        then(productCacheInvalidator).should().evictDetail(10L);
    }

    @Test
    void approvedProductUpdatesPolicyAndCreatesContentRevision() {
        ProductUpdateRequest request = request("변경 상품", List.of());
        givenOwnedProduct(ProductStatus.APPROVED);
        given(category.getId()).willReturn(2L);
        given(product.getId()).willReturn(10L);
        given(product.getCategory()).willReturn(category);
        given(product.getName()).willReturn("기존 상품");
        given(productRevisionRepository.findByProductIdAndStatus(
            10L,
            ProductStatus.PENDING
        )).willReturn(Optional.empty());
        given(productMapper.toProductDetailResponse(product)).willReturn(response);

        sellerProductService.updateProduct(1L, 10L, request);

        ArgumentCaptor<ProductRevision> captor =
            ArgumentCaptor.forClass(ProductRevision.class);
        then(productRevisionRepository).should().save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("변경 상품");
        then(product).should().updateOperationalPolicy(
            request.price(),
            request.discountPercentage(),
            request.discountStartDate(),
            request.discountEndDate(),
            request.freeShipping(),
            request.shippingFee(),
            request.estimatedDeliveryDays(),
            request.stock()
        );
        then(product).should(never()).requestApproval();
        then(product).should(never()).replaceImages(anyList());
        then(productCacheInvalidator).should().evictDetail(10L);
    }

    private void givenOwnedProduct(ProductStatus status) {
        given(sellerProfileService.getProfileEntity(1L)).willReturn(sellerProfile);
        given(sellerProfile.getId()).willReturn(3L);
        given(productRepository.findByIdAndSellerProfileIdAndStatusNot(
            10L,
            3L,
            ProductStatus.DELETED
        )).willReturn(Optional.of(product));
        given(product.getStatus()).willReturn(status);
        given(categoryRepository.findById(2L)).willReturn(Optional.of(category));
        given(category.isActive()).willReturn(true);
    }

    private ProductUpdateRequest request(
        String name,
        List<ProductDetailImageCreateRequest> detailImages
    ) {
        return new ProductUpdateRequest(
            2L,
            name,
            "변경 설명",
            "변경 브랜드",
            BigDecimal.valueOf(12_000),
            BigDecimal.ZERO,
            5,
            "new-thumbnail",
            List.of(),
            detailImages
        );
    }
}
