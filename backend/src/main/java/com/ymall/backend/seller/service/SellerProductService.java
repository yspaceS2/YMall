package com.ymall.backend.seller.service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
import com.ymall.backend.product.entity.ProductRevision;
import com.ymall.backend.product.entity.ProductRevisionDetailImage;
import com.ymall.backend.product.entity.ProductRevisionImage;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.mapper.ProductMapper;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.repository.ProductRevisionRepository;
import com.ymall.backend.product.service.ProductCacheInvalidator;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.dto.SellerProductResponse;
import com.ymall.backend.seller.dto.SellerProductStockCondition;

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
    private final ProductRevisionRepository productRevisionRepository;

    public PageResponse<SellerProductResponse> getProducts(
        Long memberId,
        int page,
        int size,
        String keyword,
        Long categoryId,
        SellerProductStockCondition stockCondition,
        Integer stockQuantity
    ) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        Pageable pageable = PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        boolean filterCategory = categoryId != null;
        Set<Long> categoryIds = filterCategory
            ? getActiveCategoryTreeIds(categoryId)
            : Set.of(-1L);
        SellerProductStockCondition normalizedStockCondition = stockCondition == null
            ? SellerProductStockCondition.GTE
            : stockCondition;
        Integer minimumStock = stockQuantity != null
            && normalizedStockCondition == SellerProductStockCondition.GTE
            ? stockQuantity
            : null;
        Integer maximumStock = stockQuantity != null
            && normalizedStockCondition == SellerProductStockCondition.LTE
            ? stockQuantity
            : null;
        return PageResponse.from(productRepository.searchSellerProducts(
            profile.getId(),
            ProductStatus.DELETED,
            normalizedKeyword,
            filterCategory,
            categoryIds,
            minimumStock,
            maximumStock,
            pageable
        ).map(SellerProductResponse::from));
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
        Category category = getCategory(request.categoryId());
        if (product.getStatus() == ProductStatus.PENDING
            || product.getStatus() == ProductStatus.REJECTED) {
            product.update(
                category,
                request.name(),
                request.description(),
                request.brand(),
                request.price(),
                request.discountPercentage(),
                request.discountStartDate(),
                request.discountEndDate(),
                request.freeShipping(),
                request.shippingFee() == null ? java.math.BigDecimal.ZERO : request.shippingFee(),
                request.estimatedDeliveryDays(),
                request.stock(),
                request.thumbnailUrl()
            );
            product.replaceImages(productMapper.toImageEntities(request));
            product.replaceDetailImages(productMapper.toDetailImageEntities(request));
            product.requestApproval();
        } else {
            boolean requiresReview = contentChanged(product, category, request);
            product.updateOperationalPolicy(
                request.price(),
                request.discountPercentage(),
                request.discountStartDate(),
                request.discountEndDate(),
                request.freeShipping(),
                request.shippingFee() == null ? java.math.BigDecimal.ZERO : request.shippingFee(),
                request.estimatedDeliveryDays(),
                request.stock()
            );
            if (requiresReview) {
                savePendingRevision(product, category, request);
            }
        }
        productCacheInvalidator.evictDetail(productId);
        return productMapper.toProductDetailResponse(product);
    }

    private void savePendingRevision(
        Product product,
        Category category,
        ProductUpdateRequest request
    ) {
        ProductRevision revision = productRevisionRepository
            .findByProductIdAndStatus(product.getId(), ProductStatus.PENDING)
            .orElseGet(() -> new ProductRevision(
                product,
                category,
                request.name(),
                request.description(),
                request.brand(),
                request.thumbnailUrl()
            ));
        revision.update(
            category,
            request.name(),
            request.description(),
            request.brand(),
            request.thumbnailUrl()
        );
        revision.replaceImages(request.images() == null ? List.of() : request.images().stream()
            .map(image -> new ProductRevisionImage(
                image.originalUrl(),
                image.imageUrl(),
                image.sortOrder()
            ))
            .toList());
        revision.replaceDetailImages(request.detailImages() == null
            ? List.of()
            : request.detailImages().stream()
                .map(image -> new ProductRevisionDetailImage(
                    image.originalUrl(),
                    image.imageUrl(),
                    image.sortOrder()
                ))
                .toList());
        productRevisionRepository.save(revision);
    }

    private boolean contentChanged(
        Product product,
        Category category,
        ProductUpdateRequest request
    ) {
        return !Objects.equals(product.getCategory().getId(), category.getId())
            || !Objects.equals(product.getName(), request.name())
            || !Objects.equals(product.getDescription(), request.description())
            || !Objects.equals(product.getBrand(), request.brand())
            || !Objects.equals(product.getThumbnailUrl(), request.thumbnailUrl())
            || !product.getImages().stream()
                .sorted(java.util.Comparator.comparing(image -> image.getSortOrder()))
                .map(image -> image.getImageUrl())
                .toList()
                .equals(request.images() == null ? List.of() : request.images().stream()
                    .sorted(java.util.Comparator.comparing(image -> image.sortOrder()))
                    .map(image -> image.imageUrl())
                    .toList())
            || !product.getDetailImages().stream()
                .sorted(java.util.Comparator.comparing(image -> image.getSortOrder()))
                .map(image -> image.getImageUrl())
                .toList()
                .equals(request.detailImages() == null ? List.of() : request.detailImages().stream()
                    .sorted(java.util.Comparator.comparing(image -> image.sortOrder()))
                    .map(image -> image.imageUrl())
                    .toList());
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

    private Set<Long> getActiveCategoryTreeIds(Long rootCategoryId) {
        Category rootCategory = categoryRepository.findById(rootCategoryId)
            .filter(Category::isActive)
            .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
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
}
