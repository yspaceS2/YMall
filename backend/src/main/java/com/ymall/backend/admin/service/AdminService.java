package com.ymall.backend.admin.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
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
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.search.KoreanSearchNormalizer;
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
    private final PaymentRepository paymentRepository;
    private final AdminMapper adminMapper;
    private final ProductCacheInvalidator productCacheInvalidator;

    public PageResponse<AdminProductResponse> getProducts(
        ProductStatus status,
        int page,
        int size,
        String keyword
    ) {
        String normalizedKeyword = normalizeProductSearchKeyword(keyword);
        Pageable pageable = createPageable(page, size);
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

    private String choseongKeyword(String normalizedKeyword) {
        return KoreanSearchNormalizer.isChoseongQuery(normalizedKeyword)
            ? normalizedKeyword
            : "";
    }

    public AdminProductResponse getProduct(Long productId) {
        Product product = productRepository.findByIdForAdminView(productId)
            .filter(foundProduct -> foundProduct.getStatus() != ProductStatus.DELETED)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        return adminMapper.toProductResponse(product);
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
            if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            product.reject(request.rejectionReason());
        }
        productCacheInvalidator.evictDetail(productId);

        return adminMapper.toProductResponse(product);
    }

    public PageResponse<AdminMemberResponse> getMembers(int page, int size, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return PageResponse.from((normalizedKeyword.isEmpty()
            ? memberRepository.findAll(createPageable(page, size))
            : memberRepository.search(normalizedKeyword, createPageable(page, size)))
            .map(adminMapper::toMemberResponse));
    }

    public AdminMemberResponse getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .map(adminMapper::toMemberResponse)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public PageResponse<AdminSellerResponse> getSellers(int page, int size, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return PageResponse.from((normalizedKeyword.isEmpty()
            ? sellerProfileRepository.findAll(createPageable(page, size))
            : sellerProfileRepository.search(normalizedKeyword, createPageable(page, size)))
            .map(adminMapper::toSellerResponse));
    }

    public AdminSellerResponse getSeller(Long sellerId) {
        return sellerProfileRepository.findWithMemberById(sellerId)
            .map(adminMapper::toSellerResponse)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
    }

    public PageResponse<AdminOrderResponse> getOrders(int page, int size, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Long orderId = parseOrderId(normalizedKeyword);
        Page<Order> orders = normalizedKeyword.isEmpty()
            ? orderRepository.findAll(createPageable(page, size))
            : orderRepository.searchAdminOrders(
                orderId == null ? normalizedKeyword : "",
                orderId,
                createPageable(page, size)
            );
        return toOrderPage(orders);
    }

    public AdminOrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findAdminOrderById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        boolean refundSupported = paymentRepository
            .existsByOrderIdAndResultAndPaymentKeyIsNotNull(orderId, PaymentResult.SUCCESS);
        return adminMapper.toOrderResponse(order, refundSupported);
    }

    private PageResponse<AdminOrderResponse> toOrderPage(Page<Order> orders) {
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Set<Long> refundSupportedOrderIds = orderIds.isEmpty()
            ? Set.of()
            : paymentRepository.findRefundSupportedOrderIds(
                orderIds,
                PaymentResult.SUCCESS
            );
        return PageResponse.from(orders.map(order -> adminMapper.toOrderResponse(
            order,
            refundSupportedOrderIds.contains(order.getId())
        )));
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private String normalizeProductSearchKeyword(String keyword) {
        return KoreanSearchNormalizer.normalize(keyword);
    }

    private Long parseOrderId(String keyword) {
        try {
            return keyword.isBlank() ? null : Long.valueOf(keyword);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Pageable createPageable(int page, int size) {
        int pageNumber = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
