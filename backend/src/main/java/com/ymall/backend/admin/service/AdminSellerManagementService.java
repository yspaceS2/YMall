package com.ymall.backend.admin.service;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.admin.dto.AdminSellerResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderItemRepository;
import com.ymall.backend.order.returnrequest.entity.ReturnRequestStatus;
import com.ymall.backend.order.returnrequest.repository.ProductReturnRequestRepository;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.seller.entity.SellerApplication;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerApplicationRepository;
import com.ymall.backend.seller.repository.SellerProfileRepository;
import com.ymall.backend.settlement.entity.SettlementRequestStatus;
import com.ymall.backend.settlement.repository.SettlementRequestRepository;
import com.ymall.backend.support.entity.SupportInquiryStatus;
import com.ymall.backend.support.repository.SupportInquiryRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AdminSellerManagementService {

    private static final List<OrderStatus> PAID_ORDER_STATUSES = List.of(
        OrderStatus.PAID,
        OrderStatus.PREPARING,
        OrderStatus.SHIPPED,
        OrderStatus.DELIVERED,
        OrderStatus.PARTIALLY_REFUNDED,
        OrderStatus.REFUNDED
    );

    private static final List<SupportInquiryStatus> ACTIVE_INQUIRY_STATUSES = List.of(
        SupportInquiryStatus.WAITING,
        SupportInquiryStatus.IN_PROGRESS,
        SupportInquiryStatus.LIVE_REQUESTED,
        SupportInquiryStatus.LIVE_OFFERED,
        SupportInquiryStatus.LIVE_ACTIVE
    );

    private final SellerProfileRepository sellerProfileRepository;
    private final SellerApplicationRepository sellerApplicationRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductReturnRequestRepository returnRequestRepository;
    private final SettlementRequestRepository settlementRequestRepository;
    private final SupportInquiryRepository supportInquiryRepository;
    private final AdminAccessService accessService;
    private final AdminPageRequestFactory pageRequestFactory;

    PageResponse<AdminSellerResponse> getSellers(
        Long actorMemberId,
        int page,
        int size,
        String keyword
    ) {
        Member actor = accessService.requireAdmin(actorMemberId);
        boolean revealSensitiveData = accessService.canReadSensitiveMemberData(actor);
        String normalizedKeyword = normalize(keyword);
        return PageResponse.from((normalizedKeyword.isEmpty()
            ? sellerProfileRepository.findAll(pageRequestFactory.create(page, size))
            : sellerProfileRepository.search(
                normalizedKeyword,
                pageRequestFactory.create(page, size)
            ))
            .map(profile -> toListResponse(profile, revealSensitiveData)));
    }

    AdminSellerResponse getSeller(Long actorMemberId, Long sellerId) {
        Member actor = accessService.requireAdmin(actorMemberId);
        boolean revealSensitiveData = accessService.canReadSensitiveMemberData(actor);
        return sellerProfileRepository.findWithMemberById(sellerId)
            .map(profile -> toDetailResponse(profile, revealSensitiveData))
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
    }

    private AdminSellerResponse toListResponse(
        SellerProfile profile,
        boolean revealSensitiveData
    ) {
        return new AdminSellerResponse(
            profile.getId(),
            profile.getMember().getId(),
            revealSensitiveData
                ? profile.getMember().getEmail()
                : accessService.maskEmail(profile.getMember().getEmail()),
            profile.getMember().getName(),
            profile.getStoreName(),
            revealSensitiveData
                ? profile.getBusinessNumber()
                : accessService.maskBusinessNumber(profile.getBusinessNumber()),
            0L, 0L, 0L, BigDecimal.ZERO, 0L, 0L, 0L, 0L,
            null, null, null,
            profile.getCreatedAt()
        );
    }

    private AdminSellerResponse toDetailResponse(
        SellerProfile profile,
        boolean revealSensitiveData
    ) {
        Long sellerProfileId = profile.getId();
        SellerApplication application = sellerApplicationRepository
            .findByMemberId(profile.getMember().getId())
            .orElse(null);
        return new AdminSellerResponse(
            sellerProfileId,
            profile.getMember().getId(),
            revealSensitiveData
                ? profile.getMember().getEmail()
                : accessService.maskEmail(profile.getMember().getEmail()),
            profile.getMember().getName(),
            profile.getStoreName(),
            revealSensitiveData
                ? profile.getBusinessNumber()
                : accessService.maskBusinessNumber(profile.getBusinessNumber()),
            productRepository.countBySellerProfileIdAndStatusNot(
                sellerProfileId,
                ProductStatus.DELETED
            ),
            productRepository.countBySellerProfileIdAndStatus(
                sellerProfileId,
                ProductStatus.PENDING
            ),
            orderItemRepository.countSellerOrders(sellerProfileId, PAID_ORDER_STATUSES),
            orderItemRepository.sumSellerGrossSales(sellerProfileId, PAID_ORDER_STATUSES),
            orderItemRepository.sumSellerRefundedQuantity(sellerProfileId),
            returnRequestRepository.countBySellerProfileIdAndStatus(
                sellerProfileId,
                ReturnRequestStatus.REQUESTED
            ),
            supportInquiryRepository.countByMemberIdAndStatusIn(
                profile.getMember().getId(),
                ACTIVE_INQUIRY_STATUSES
            ),
            settlementRequestRepository.countBySellerProfileIdAndStatus(
                sellerProfileId,
                SettlementRequestStatus.REQUESTED
            ),
            application == null ? null : application.getStatus(),
            application == null ? null : application.getRejectionReason(),
            application == null ? null : application.getReviewedAt(),
            profile.getCreatedAt()
        );
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }
}
