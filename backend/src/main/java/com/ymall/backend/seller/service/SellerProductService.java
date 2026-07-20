package com.ymall.backend.seller.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.config.ProductCacheNames;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
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
import com.ymall.backend.seller.entity.SellerProfile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SellerProfileService sellerProfileService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public PageResponse<ProductListResponse> getProducts(Long memberId, int page, int size) {
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
            ).map(productMapper::toProductListResponse)
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
    @CacheEvict(cacheNames = ProductCacheNames.DETAILS, key = "#productId", beforeInvocation = true)
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
        product.requestApproval();
        return productMapper.toProductDetailResponse(product);
    }

    @Transactional
    @CacheEvict(cacheNames = ProductCacheNames.DETAILS, key = "#productId", beforeInvocation = true)
    public void deleteProduct(Long memberId, Long productId) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        getOwnedProduct(profile.getId(), productId).delete();
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
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
