package com.ymall.backend.seller.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.mapper.ProductMapper;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
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
    @DisplayName("판매자가 상품을 수정하면 상세 이미지도 교체한다")
    void updateProductReplacesDetailImages() {
        ProductUpdateRequest request = new ProductUpdateRequest(
            2L,
            "수정 상품",
            "수정 설명",
            "수정 브랜드",
            BigDecimal.valueOf(10_000),
            BigDecimal.TEN,
            5,
            "thumbnail",
            List.of(),
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

        given(sellerProfileService.getProfileEntity(1L)).willReturn(sellerProfile);
        given(sellerProfile.getId()).willReturn(3L);
        given(productRepository.findByIdAndSellerProfileIdAndStatusNot(
            10L,
            3L,
            ProductStatus.DELETED
        )).willReturn(Optional.of(product));
        given(categoryRepository.findById(2L)).willReturn(Optional.of(category));
        given(productMapper.toImageEntities(request)).willReturn(images);
        given(productMapper.toDetailImageEntities(request)).willReturn(detailImages);
        given(productMapper.toProductDetailResponse(product)).willReturn(response);

        sellerProductService.updateProduct(1L, 10L, request);

        then(product).should().replaceImages(images);
        then(product).should().replaceDetailImages(detailImages);
        then(productCacheInvalidator).should().evictDetail(10L);
    }
}
