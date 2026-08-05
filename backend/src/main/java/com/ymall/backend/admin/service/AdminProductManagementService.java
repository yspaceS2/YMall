package com.ymall.backend.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.admin.dto.AdminProductResponse;
import com.ymall.backend.admin.dto.AdminProductStatusUpdateRequest;
import com.ymall.backend.admin.mapper.AdminMapper;
import com.ymall.backend.dashboard.service.DashboardRealtimePublisher;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.search.KoreanSearchNormalizer;
import com.ymall.backend.product.service.ProductCacheInvalidator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AdminProductManagementService {

    private final ProductRepository productRepository;
    private final AdminMapper adminMapper;
    private final ProductCacheInvalidator productCacheInvalidator;
    private final DashboardRealtimePublisher dashboardRealtimePublisher;
    private final AdminPageRequestFactory pageRequestFactory;

    PageResponse<AdminProductResponse> getProducts(
        ProductStatus status,
        int page,
        int size,
        String keyword
    ) {
        String normalizedKeyword = KoreanSearchNormalizer.normalize(keyword);
        Pageable pageable = pageRequestFactory.create(page, size);
        return PageResponse.from(
            (normalizedKeyword.isEmpty()
                ? productRepository.findByStatus(status, pageable)
                : productRepository.searchAdminProducts(
                    status,
                    normalizedKeyword,
                    choseongKeyword(normalizedKeyword),
                    pageable
                ))
                .map(adminMapper::toProductListResponse)
        );
    }

    AdminProductResponse getProduct(Long productId) {
        Product product = productRepository.findByIdForAdminView(productId)
            .filter(foundProduct -> foundProduct.getStatus() != ProductStatus.DELETED)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return adminMapper.toProductResponse(product);
    }

    @Transactional
    AdminProductResponse updateProductStatus(
        Long productId,
        AdminProductStatusUpdateRequest request
    ) {
        if (request.status() != ProductStatus.APPROVED
            && request.status() != ProductStatus.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Product product = productRepository.findByIdForReview(productId)
            .filter(foundProduct -> foundProduct.getStatus() != ProductStatus.DELETED)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStatus() != ProductStatus.PENDING) {
            throw new BusinessException(ErrorCode.PRODUCT_REVIEW_NOT_ALLOWED);
        }
        if (request.status() == ProductStatus.APPROVED) {
            product.approve();
        } else {
            if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            product.reject(request.rejectionReason());
        }
        productCacheInvalidator.evictDetail(productId);
        dashboardRealtimePublisher.invalidateSellerAndAdmins(
            product.getSellerProfile().getMember().getId(),
            "product",
            productId
        );
        return adminMapper.toProductResponse(product);
    }

    private String choseongKeyword(String normalizedKeyword) {
        return KoreanSearchNormalizer.isChoseongQuery(normalizedKeyword)
            ? normalizedKeyword
            : "";
    }
}
