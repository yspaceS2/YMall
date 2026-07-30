package com.ymall.backend.seller.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.dto.ProductCreateRequest;
import com.ymall.backend.product.dto.ProductDetailResponse;
import com.ymall.backend.product.dto.ProductUpdateRequest;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.mapper.ProductMapper;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.service.ProductCacheInvalidator;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.dto.SellerProductResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SellerProfileService sellerProfileService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductCacheInvalidator productCacheInvalidator;

    public PageResponse<SellerProductResponse> getProducts(Long memberId, int page, int size) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        Pageable pageable = PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return PageResponse.from(
            productRepository.findBySellerProfileIdAndStatusNot(
                profile.getId(),
                ProductStatus.DELETED,
                pageable
            ).map(SellerProductResponse::from)
        );
    }

    public ProductDetailResponse getProduct(Long memberId, Long productId) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        return productMapper.toProductDetailResponse(getOwnedProduct(profile.getId(), productId));
    }

    @Transactional
    public ProductDetailResponse createProduct(Long memberId, ProductCreateRequest request) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        Category category = getCategory(request.categoryId());
        Product product = productMapper.toEntity(request, category, ProductStatus.PENDING);
        product.assignSellerProfile(profile);
        return productMapper.toProductDetailResponse(productRepository.save(product));
    }

    @Transactional
    public ProductDetailResponse updateProduct(
        Long memberId,
        Long productId,
        ProductUpdateRequest request
    ) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        Product product = getOwnedProduct(profile.getId(), productId);
        product.update(
            getCategory(request.categoryId()),
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
        product.requestApproval();
        productCacheInvalidator.evictDetail(productId);
        return productMapper.toProductDetailResponse(product);
    }

    @Transactional
    public void deleteProduct(Long memberId, Long productId) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        getOwnedProduct(profile.getId(), productId).delete();
        productCacheInvalidator.evictDetail(productId);
    }

    private Product getOwnedProduct(Long sellerProfileId, Long productId) {
        return productRepository.findByIdAndSellerProfileIdAndStatusNot(
                productId,
                sellerProfileId,
                ProductStatus.DELETED
            )
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PRODUCT_NOT_FOUND));
    }

    private Category getCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (!isCategoryPathActive(category)
            || categoryRepository.existsByParentId(category.getId())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_SELECTABLE);
        }
        return category;
    }

    private boolean isCategoryPathActive(Category category) {
        Category cursor = category;
        while (cursor != null) {
            if (!cursor.isActive()) {
                return false;
            }
            cursor = cursor.getParent();
        }
        return true;
    }
}
