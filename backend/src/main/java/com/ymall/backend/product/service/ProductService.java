package com.ymall.backend.product.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.config.ProductCacheNames;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.dto.CategoryResponse;
import com.ymall.backend.product.dto.ProductCreateRequest;
import com.ymall.backend.product.dto.ProductDetailResponse;
import com.ymall.backend.product.dto.ProductListResponse;
import com.ymall.backend.product.dto.ProductUpdateRequest;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.mapper.ProductMapper;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductCacheInvalidator productCacheInvalidator;

    /**
     * 사용자에게 노출되는 상품 목록은 승인된 상품만 대상으로 한다.
     * 판매자 검수나 삭제 상태의 상품은 별도 관리 API에서 다루도록 분리한다.
     */
    public PageResponse<ProductListResponse> getProducts(int page, int size) {
        Pageable pageable = createPageable(page, size);

        return PageResponse.from(
            productRepository.findByStatus(ProductStatus.APPROVED, pageable)
                .map(productMapper::toProductListResponse)
        );
    }

    /**
     * 상품 상세 조회도 공개 목록과 동일하게 APPROVED 상태만 허용한다.
     * DELETED, DRAFT, PENDING 상품은 외부 사용자가 존재 여부를 알 수 없도록 404로 처리한다.
     */
    @Cacheable(cacheNames = ProductCacheNames.DETAILS, key = "#productId", sync = true)
    public ProductDetailResponse getProduct(Long productId) {
        return productRepository.findWithCategoryAndImagesById(productId)
            .filter(product -> product.getStatus() == ProductStatus.APPROVED)
            .map(productMapper::toProductDetailResponse)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public PageResponse<ProductListResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = createPageable(page, size);

        return PageResponse.from(
            productRepository.findByNameContainingIgnoreCaseAndStatus(
                    keyword,
                    ProductStatus.APPROVED,
                    pageable
                )
                .map(productMapper::toProductListResponse)
        );
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
            .stream()
            .map(productMapper::toCategoryResponse)
            .toList();
    }

    public PageResponse<ProductListResponse> getProductsByCategory(Long categoryId, int page, int size) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        return PageResponse.from(
            productRepository.findByCategoryAndStatus(
                    category,
                    ProductStatus.APPROVED,
                    createPageable(page, size)
                )
                .map(productMapper::toProductListResponse)
        );
    }

    @Transactional
    public ProductDetailResponse createProduct(ProductCreateRequest request) {
        Category category = getCategory(request.categoryId());
        Product product = productMapper.toEntity(request, category, ProductStatus.APPROVED);
        Product savedProduct = productRepository.save(product);

        return productMapper.toProductDetailResponse(savedProduct);
    }

    /**
     * 상품 수정은 요청에 포함된 이미지 목록을 기준으로 전체 교체한다.
     * 부분 수정 방식은 이미지 정렬과 삭제 처리가 복잡해지므로 MVP에서는 전체 교체 정책을 사용한다.
     */
    @Transactional
    public ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = getProductEntity(productId);
        Category category = getCategory(request.categoryId());

        product.update(
            category,
            request.name(),
            request.description(),
            request.brand(),
            request.price(),
            request.discountPercentage(),
            request.stock(),
            request.thumbnailUrl()
        );
        product.replaceImages(productMapper.toImageEntities(request));
        product.replaceDetailImages(productMapper.toDetailImageEntities(request));

        productCacheInvalidator.evictDetail(productId);

        return productMapper.toProductDetailResponse(product);
    }

    /**
     * 상품 삭제는 주문, 장바구니, 리뷰 등 참조 이력 보존을 위해 물리 삭제하지 않는다.
     * 상태만 DELETED로 전환하고 공개 조회 쿼리에서 제외한다.
     */
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = getProductEntity(productId);

        product.delete();
        productCacheInvalidator.evictDetail(productId);
    }

    /**
     * 프론트엔드는 1부터 시작하는 page 값을 사용하고,
     * Spring Data PageRequest는 0부터 시작하므로 여기에서 변환한다.
     * 과도한 요청을 막기 위해 size는 MAX_PAGE_SIZE 이하로 제한한다.
     */
    private Pageable createPageable(int page, int size) {
        int pageNumber = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    /**
     * 수정과 삭제 대상 상품은 공개 상태뿐 아니라 비공개 상태도 관리 대상이 될 수 있다.
     * 다만 이미 삭제 처리된 상품은 재수정/재삭제를 막기 위해 조회 대상에서 제외한다.
     */
    private Product getProductEntity(Long productId) {
        return productRepository.findWithCategoryAndImagesById(productId)
            .filter(product -> product.getStatus() != ProductStatus.DELETED)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
