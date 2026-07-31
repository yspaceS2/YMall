package com.ymall.backend.product.service;

import java.math.BigDecimal;
import java.util.Comparator;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.admin.dto.AdminProductChangeStatusUpdateRequest;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.dto.ProductChangeRequestResponse;
import com.ymall.backend.product.dto.ProductImageSnapshot;
import com.ymall.backend.product.dto.ProductSnapshotResponse;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductDetailImage;
import com.ymall.backend.product.entity.ProductImage;
import com.ymall.backend.product.entity.ProductRevision;
import com.ymall.backend.product.entity.ProductRevisionDetailImage;
import com.ymall.backend.product.entity.ProductRevisionImage;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRevisionRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductChangeReviewService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRevisionRepository revisionRepository;
    private final ProductCacheInvalidator productCacheInvalidator;

    public PageResponse<ProductChangeRequestResponse> getRequests(
        ProductStatus status,
        int page,
        int size
    ) {
        return PageResponse.from(revisionRepository.search(
            status,
            "",
            PageRequest.of(
                Math.max(page - 1, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt")
            )
        ).map(this::toResponse));
    }

    public ProductChangeRequestResponse getRequest(Long requestId) {
        return revisionRepository.findDetailById(requestId)
            .map(this::toResponse)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.PRODUCT_CHANGE_REQUEST_NOT_FOUND
            ));
    }

    @Transactional
    public ProductChangeRequestResponse review(
        Long requestId,
        AdminProductChangeStatusUpdateRequest request
    ) {
        if (request.status() != ProductStatus.APPROVED
            && request.status() != ProductStatus.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        ProductRevision revision = revisionRepository.findByIdForReview(requestId)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.PRODUCT_CHANGE_REQUEST_NOT_FOUND
            ));
        if (revision.getStatus() != ProductStatus.PENDING) {
            throw new BusinessException(ErrorCode.PRODUCT_CHANGE_REVIEW_NOT_ALLOWED);
        }

        if (request.status() == ProductStatus.APPROVED) {
            apply(revision);
            revision.approve();
            productCacheInvalidator.evictDetail(revision.getProduct().getId());
        } else {
            if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            revision.reject(request.rejectionReason().trim());
        }
        return toResponse(revision);
    }

    private void apply(ProductRevision revision) {
        Product product = revision.getProduct();
        product.applyApprovedContent(
            revision.getCategory(),
            revision.getName(),
            revision.getDescription(),
            revision.getBrand(),
            revision.getThumbnailUrl()
        );
        product.replaceImages(revision.getImages().stream()
            .sorted(Comparator.comparing(ProductRevisionImage::getSortOrder))
            .map(image -> new ProductImage(
                image.getOriginalUrl(),
                image.getImageUrl(),
                image.getSortOrder()
            ))
            .toList());
        product.replaceDetailImages(revision.getDetailImages().stream()
            .sorted(Comparator.comparing(ProductRevisionDetailImage::getSortOrder))
            .map(image -> new ProductDetailImage(
                image.getOriginalUrl(),
                image.getImageUrl(),
                image.getSortOrder()
            ))
            .toList());
    }

    private ProductChangeRequestResponse toResponse(ProductRevision revision) {
        Product product = revision.getProduct();
        return new ProductChangeRequestResponse(
            revision.getId(),
            product.getId(),
            product.getSellerProfile() == null ? null : product.getSellerProfile().getId(),
            product.getSellerProfile() == null ? null : product.getSellerProfile().getStoreName(),
            revision.getStatus(),
            currentSnapshot(product),
            proposedSnapshot(revision),
            revision.getRejectionReason(),
            revision.getCreatedAt(),
            revision.getReviewedAt()
        );
    }

    private ProductSnapshotResponse currentSnapshot(Product product) {
        return new ProductSnapshotResponse(
            product.getCategory().getId(),
            product.getCategory().getName(),
            product.getName(),
            product.getDescription(),
            product.getBrand(),
            product.getPrice(),
            valueOrZero(product.getDiscountPercentage()),
            product.getDiscountStartDate(),
            product.getDiscountEndDate(),
            product.getStock(),
            product.getThumbnailUrl(),
            product.isFreeShipping(),
            product.getEffectiveShippingFee(),
            product.getEstimatedDeliveryDays(),
            product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getSortOrder))
                .map(image -> new ProductImageSnapshot(
                    image.getOriginalUrl(),
                    image.getImageUrl(),
                    image.getSortOrder()
                ))
                .toList(),
            product.getDetailImages().stream()
                .sorted(Comparator.comparing(ProductDetailImage::getSortOrder))
                .map(image -> new ProductImageSnapshot(
                    image.getOriginalUrl(),
                    image.getImageUrl(),
                    image.getSortOrder()
                ))
                .toList()
        );
    }

    private ProductSnapshotResponse proposedSnapshot(ProductRevision revision) {
        Product product = revision.getProduct();
        return new ProductSnapshotResponse(
            revision.getCategory().getId(),
            revision.getCategory().getName(),
            revision.getName(),
            revision.getDescription(),
            revision.getBrand(),
            product.getPrice(),
            valueOrZero(product.getDiscountPercentage()),
            product.getDiscountStartDate(),
            product.getDiscountEndDate(),
            product.getStock(),
            revision.getThumbnailUrl(),
            product.isFreeShipping(),
            product.getEffectiveShippingFee(),
            product.getEstimatedDeliveryDays(),
            revision.getImages().stream()
                .sorted(Comparator.comparing(ProductRevisionImage::getSortOrder))
                .map(image -> new ProductImageSnapshot(
                    image.getOriginalUrl(),
                    image.getImageUrl(),
                    image.getSortOrder()
                ))
                .toList(),
            revision.getDetailImages().stream()
                .sorted(Comparator.comparing(ProductRevisionDetailImage::getSortOrder))
                .map(image -> new ProductImageSnapshot(
                    image.getOriginalUrl(),
                    image.getImageUrl(),
                    image.getSortOrder()
                ))
                .toList()
        );
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
