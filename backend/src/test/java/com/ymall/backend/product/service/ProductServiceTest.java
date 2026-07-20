package com.ymall.backend.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.dto.CategoryResponse;
import com.ymall.backend.product.dto.ProductCreateRequest;
import com.ymall.backend.product.dto.ProductDetailResponse;
import com.ymall.backend.product.dto.ProductImageCreateRequest;
import com.ymall.backend.product.dto.ProductUpdateRequest;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductImage;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.mapper.ProductMapper;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductCacheInvalidator productCacheInvalidator;

    @InjectMocks
    private ProductService productService;

    /**
     * 상품 생성 요청이 들어왔을 때 정상적으로 카테고리를 조회하고,
     * ProductCreateRequest를 Product 엔티티로 변환한 뒤 저장하는지 검증한다.
     *
     * 검증 범위:
     * - categoryRepository.findById 호출 결과를 사용한다.
     * - productMapper.toEntity로 DTO를 Entity로 변환한다.
     * - productRepository.save로 상품을 저장한다.
     * - 저장된 상품을 ProductDetailResponse로 변환해 반환한다.
     */
    @Test
    @DisplayName("상품을 등록한다")
    void createProduct() {
        Category category = new Category("smartphones", "smartphones");
        ProductCreateRequest request = createRequest();
        Product product = createProduct(category);
        ProductDetailResponse response = createDetailResponse();

        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
        given(productMapper.toEntity(request, category, ProductStatus.APPROVED)).willReturn(product);
        given(productRepository.save(product)).willReturn(product);
        given(productMapper.toProductDetailResponse(product)).willReturn(response);

        ProductDetailResponse result = productService.createProduct(request);

        assertThat(result.name()).isEqualTo("iPhone 15");
        then(productRepository).should().save(product);
    }

    /**
     * 상품 생성 요청의 categoryId에 해당하는 카테고리가 없을 때
     * 상품을 저장하지 않고 CATEGORY_NOT_FOUND 예외를 발생시키는지 검증한다.
     *
     * 검증 범위:
     * - categoryRepository.findById가 Optional.empty를 반환한다.
     * - ProductService는 BusinessException을 던진다.
     * - 예외의 ErrorCode는 CATEGORY_NOT_FOUND이다.
     */
    @Test
    @DisplayName("존재하지 않는 카테고리로 상품을 등록하면 예외가 발생한다")
    void createProductWithMissingCategory() {
        ProductCreateRequest request = createRequest();

        given(categoryRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    /**
     * 기존 상품을 수정할 때 상품 엔티티의 기본 정보와 이미지 목록이
     * ProductUpdateRequest 기준으로 교체되는지 검증한다.
     *
     * 검증 범위:
     * - 기존 상품을 조회한다.
     * - 새 카테고리를 조회한다.
     * - 요청 DTO의 이미지 목록을 ProductImage 엔티티 목록으로 변환한다.
     * - 상품명과 이미지 목록이 수정 결과에 반영된다.
     */
    @Test
    @DisplayName("상품을 수정한다")
    void updateProduct() {
        Category oldCategory = new Category("smartphones", "smartphones");
        Category newCategory = new Category("laptops", "laptops");
        Product product = createProduct(oldCategory);
        ProductUpdateRequest request = updateRequest();
        ProductImage newImage = new ProductImage("original", "updated-image", 0);
        ProductDetailResponse response = createUpdatedDetailResponse();

        given(productRepository.findWithCategoryAndImagesById(1L)).willReturn(Optional.of(product));
        given(categoryRepository.findById(2L)).willReturn(Optional.of(newCategory));
        given(productMapper.toImageEntities(request)).willReturn(List.of(newImage));
        given(productMapper.toProductDetailResponse(product)).willReturn(response);

        ProductDetailResponse result = productService.updateProduct(1L, request);

        assertThat(result.name()).isEqualTo("Updated Product");
        assertThat(product.getName()).isEqualTo("Updated Product");
        assertThat(product.getImages()).hasSize(1);
    }

    /**
     * 상품 삭제 요청이 들어왔을 때 실제 DB row를 삭제하지 않고,
     * 상품 상태만 DELETED로 변경하는 soft delete 정책을 검증한다.
     *
     * 검증 범위:
     * - 기존 상품을 조회한다.
     * - product.delete가 호출된 결과로 상태가 DELETED가 된다.
     * - repository.delete는 사용하지 않는다.
     */
    @Test
    @DisplayName("상품을 삭제하면 상태를 DELETED로 변경한다")
    void deleteProduct() {
        Category category = new Category("smartphones", "smartphones");
        Product product = createProduct(category);

        given(productRepository.findWithCategoryAndImagesById(1L)).willReturn(Optional.of(product));

        productService.deleteProduct(1L);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
    }

    /**
     * 상품 상세 조회 시 productId에 해당하는 상품이 없으면
     * PRODUCT_NOT_FOUND 예외를 발생시키는지 검증한다.
     *
     * 검증 범위:
     * - productRepository.findWithCategoryAndImagesById가 Optional.empty를 반환한다.
     * - ProductService는 BusinessException을 던진다.
     * - 예외의 ErrorCode는 PRODUCT_NOT_FOUND이다.
     */
    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 예외가 발생한다")
    void getMissingProduct() {
        given(productRepository.findWithCategoryAndImagesById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(1L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    private ProductCreateRequest createRequest() {
        return new ProductCreateRequest(
            1L,
            "iPhone 15",
            "Apple smartphone",
            "Apple",
            BigDecimal.valueOf(1200),
            BigDecimal.valueOf(10),
            20,
            "thumbnail",
            List.of(new ProductImageCreateRequest("original", "image", 0))
        );
    }

    private ProductUpdateRequest updateRequest() {
        return new ProductUpdateRequest(
            2L,
            "Updated Product",
            "Updated description",
            "Updated brand",
            BigDecimal.valueOf(900),
            BigDecimal.valueOf(5),
            10,
            "updated-thumbnail",
            List.of(new ProductImageCreateRequest("original", "updated-image", 0))
        );
    }

    private Product createProduct(Category category) {
        Product product = new Product(
            category,
            "iPhone 15",
            "Apple smartphone",
            "Apple",
            BigDecimal.valueOf(1200),
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(4.7),
            20,
            "thumbnail",
            ProductStatus.APPROVED
        );
        product.addImage(new ProductImage("original", "image", 0));

        return product;
    }

    private ProductDetailResponse createDetailResponse() {
        return new ProductDetailResponse(
            1L,
            new CategoryResponse(1L, "smartphones", "smartphones"),
            "iPhone 15",
            "Apple smartphone",
            "Apple",
            BigDecimal.valueOf(1200),
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(4.7),
            20,
            "thumbnail",
            ProductStatus.APPROVED,
            List.of()
        );
    }

    private ProductDetailResponse createUpdatedDetailResponse() {
        return new ProductDetailResponse(
            1L,
            new CategoryResponse(2L, "laptops", "laptops"),
            "Updated Product",
            "Updated description",
            "Updated brand",
            BigDecimal.valueOf(900),
            BigDecimal.valueOf(5),
            BigDecimal.valueOf(4.7),
            10,
            "updated-thumbnail",
            ProductStatus.APPROVED,
            List.of()
        );
    }
}
