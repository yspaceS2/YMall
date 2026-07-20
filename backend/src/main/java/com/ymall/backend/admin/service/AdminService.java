package com.ymall.backend.admin.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.admin.dto.AdminMemberResponse;
import com.ymall.backend.admin.dto.AdminOrderResponse;
import com.ymall.backend.admin.dto.AdminProductResponse;
import com.ymall.backend.admin.dto.AdminProductStatusUpdateRequest;
import com.ymall.backend.admin.dto.AdminSellerResponse;
import com.ymall.backend.admin.mapper.AdminMapper;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.service.ProductCacheInvalidator;
import com.ymall.backend.seller.repository.SellerProfileRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final OrderRepository orderRepository;
    private final AdminMapper adminMapper;
    private final ProductCacheInvalidator productCacheInvalidator;

    public PageResponse<AdminProductResponse> getProducts(
        ProductStatus status,
        int page,
        int size
    ) {
        return PageResponse.from(
            productRepository.findByStatus(status, createPageable(page, size))
                .map(adminMapper::toProductResponse)
        );
    }

    @Transactional
    public AdminProductResponse updateProductStatus(
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
            product.reject();
        }
        productCacheInvalidator.evictDetail(productId);

        return adminMapper.toProductResponse(product);
    }

    public PageResponse<AdminMemberResponse> getMembers(int page, int size) {
        return PageResponse.from(
            memberRepository.findAll(createPageable(page, size))
                .map(adminMapper::toMemberResponse)
        );
    }

    public PageResponse<AdminSellerResponse> getSellers(int page, int size) {
        return PageResponse.from(
            sellerProfileRepository.findAll(createPageable(page, size))
                .map(adminMapper::toSellerResponse)
        );
    }

    public PageResponse<AdminOrderResponse> getOrders(int page, int size) {
        return PageResponse.from(
            orderRepository.findAll(createPageable(page, size))
                .map(adminMapper::toOrderResponse)
        );
    }

    private Pageable createPageable(int page, int size) {
        int pageNumber = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
