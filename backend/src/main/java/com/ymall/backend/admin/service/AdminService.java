package com.ymall.backend.admin.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.admin.dto.AdminMemberResponse;
import com.ymall.backend.admin.dto.AdminAuditLogResponse;
import com.ymall.backend.admin.dto.AdminMemberRestrictionRequest;
import com.ymall.backend.admin.dto.AdminSessionRevokeRequest;
import com.ymall.backend.admin.dto.AdminOrderResponse;
import com.ymall.backend.admin.dto.AdminProductResponse;
import com.ymall.backend.admin.dto.AdminProductStatusUpdateRequest;
import com.ymall.backend.admin.dto.AdminSellerResponse;
import com.ymall.backend.admin.mapper.AdminMapper;
import com.ymall.backend.admin.entity.AdminAuditAction;
import com.ymall.backend.admin.entity.AdminAuditLog;
import com.ymall.backend.admin.entity.AdminAuditTargetType;
import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.admin.entity.AdminPermission;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.dashboard.service.DashboardRealtimePublisher;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAccessStatus;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.order.repository.OrderItemRepository;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.returnrequest.entity.ReturnRequestStatus;
import com.ymall.backend.order.returnrequest.repository.ProductReturnRequestRepository;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.search.KoreanSearchNormalizer;
import com.ymall.backend.product.service.ProductCacheInvalidator;
import com.ymall.backend.seller.repository.SellerProfileRepository;
import com.ymall.backend.seller.repository.SellerApplicationRepository;
import com.ymall.backend.seller.entity.SellerApplication;
import com.ymall.backend.settlement.entity.SettlementRequestStatus;
import com.ymall.backend.settlement.repository.SettlementRequestRepository;
import com.ymall.backend.support.entity.SupportInquiryStatus;
import com.ymall.backend.support.repository.SupportInquiryRepository;

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
    private final DashboardRealtimePublisher dashboardRealtimePublisher;
    private final RefreshTokenService refreshTokenService;
    private final AdminAuditService auditService;
    private final com.ymall.backend.admin.repository.AdminAuditLogRepository auditLogRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductReturnRequestRepository returnRequestRepository;
    private final SettlementRequestRepository settlementRequestRepository;
    private final SupportInquiryRepository supportInquiryRepository;
    private final SellerApplicationRepository sellerApplicationRepository;

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
        dashboardRealtimePublisher.invalidateSellerAndAdmins(
            product.getSellerProfile().getMember().getId(),
            "product",
            productId
        );

        return adminMapper.toProductResponse(product);
    }

    public PageResponse<AdminMemberResponse> getMembers(
        Long actorMemberId,
        int page,
        int size,
        String keyword,
        MemberAccessStatus accessStatus,
        MemberRole role,
        AdminGrade adminGrade,
        LocalDate joinedFrom,
        LocalDate joinedTo
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Member actor = requireAdmin(actorMemberId);
        Page<Member> members = memberRepository.findAll(
            memberSpecification(
                normalizedKeyword,
                accessStatus,
                role,
                adminGrade,
                joinedFrom == null ? null : joinedFrom.atStartOfDay(),
                joinedTo == null ? null : joinedTo.plusDays(1).atStartOfDay()
            ),
            createPageable(page, size)
        );
        return toMemberPage(members, canReadSensitiveMemberData(actor));
    }

    private Specification<Member> memberSpecification(
        String keyword,
        MemberAccessStatus accessStatus,
        MemberRole role,
        AdminGrade adminGrade,
        LocalDateTime createdFrom,
        LocalDateTime createdTo
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!keyword.isBlank()) {
                String keywordPattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                    builder.like(builder.lower(root.get("name")), keywordPattern),
                    builder.like(builder.lower(root.get("email")), keywordPattern)
                ));
            }
            if (accessStatus != null) {
                predicates.add(builder.equal(root.get("accessStatus"), accessStatus));
            }
            if (role != null) {
                predicates.add(builder.equal(root.get("role"), role));
            }
            if (adminGrade != null) {
                predicates.add(builder.equal(root.get("adminGrade"), adminGrade));
            }
            if (createdFrom != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                    root.get("createdAt"), createdFrom
                ));
            }
            if (createdTo != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), createdTo));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    public AdminMemberResponse getMember(Long actorMemberId, Long memberId) {
        Member actor = requireAdmin(actorMemberId);
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return toMemberResponseWithMetrics(member, canReadSensitiveMemberData(actor));
    }

    @Transactional
    public AdminMemberResponse changeMemberRestriction(
        Long actorMemberId,
        Long memberId,
        AdminMemberRestrictionRequest request
    ) {
        Member actor = memberRepository.findByIdForUpdate(actorMemberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Member target = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        validateMemberOperation(actor, target);
        MemberAccessStatus beforeStatus = target.getAccessStatus();
        MemberAccessStatus afterStatus = request.restricted()
            ? MemberAccessStatus.RESTRICTED
            : MemberAccessStatus.ACTIVE;
        if (beforeStatus == afterStatus) {
            throw new BusinessException(ErrorCode.MEMBER_OPERATION_INVALID);
        }
        String reason = request.reason().trim();
        if (request.restricted()) target.restrict(actor, reason);
        else target.restoreAccess();
        auditService.record(
            actor,
            AdminAuditTargetType.MEMBER,
            target.getId(),
            AdminAuditAction.MEMBER_RESTRICTION_CHANGED,
            beforeStatus.name(),
            afterStatus.name(),
            reason
        );
        refreshTokenService.revokeAll(target.getId());
        return toMemberResponseWithMetrics(target, canReadSensitiveMemberData(actor));
    }

    @Transactional
    public void revokeMemberSessions(
        Long actorMemberId,
        Long memberId,
        AdminSessionRevokeRequest request
    ) {
        Member actor = memberRepository.findByIdForUpdate(actorMemberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Member target = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        validateMemberOperation(actor, target);
        target.revokeSessions();
        auditService.record(
            actor,
            AdminAuditTargetType.MEMBER,
            target.getId(),
            AdminAuditAction.MEMBER_SESSIONS_REVOKED,
            null,
            null,
            request.reason().trim()
        );
        refreshTokenService.revokeAll(target.getId());
    }

    public List<AdminAuditLogResponse> getMemberAuditLogs(Long actorMemberId, Long memberId) {
        Member actor = requireAdmin(actorMemberId);
        List<AdminAuditLog> logs = actor.getAdminGrade().hasPermission(AdminPermission.AUDIT_ALL_READ)
            ? auditLogRepository.findTop20ByTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(
                AdminAuditTargetType.MEMBER,
                memberId
            )
            : auditLogRepository.findTop20ByActorIdAndTargetTypeAndTargetIdOrderByCreatedAtDescIdDesc(
                actorMemberId,
                AdminAuditTargetType.MEMBER,
                memberId
            );
        return logs.stream().map(this::toAuditLogResponse).toList();
    }

    public PageResponse<AdminSellerResponse> getSellers(
        Long actorMemberId,
        int page,
        int size,
        String keyword
    ) {
        boolean revealSensitiveData = canReadSensitiveMemberData(requireAdmin(actorMemberId));
        String normalizedKeyword = normalizeKeyword(keyword);
        return PageResponse.from((normalizedKeyword.isEmpty()
            ? sellerProfileRepository.findAll(createPageable(page, size))
            : sellerProfileRepository.search(normalizedKeyword, createPageable(page, size)))
            .map(profile -> toSellerListResponse(profile, revealSensitiveData)));
    }

    public AdminSellerResponse getSeller(Long actorMemberId, Long sellerId) {
        boolean revealSensitiveData = canReadSensitiveMemberData(requireAdmin(actorMemberId));
        return sellerProfileRepository.findWithMemberById(sellerId)
            .map(profile -> toSellerDetailResponse(profile, revealSensitiveData))
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
    }

    public PageResponse<AdminOrderResponse> getOrders(
        int page,
        int size,
        String keyword,
        String workType
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Long orderId = parseOrderId(normalizedKeyword);
        Page<Order> orders = orderRepository.searchAdminOrders(
            orderId == null ? normalizedKeyword : "",
            orderId,
            "PENDING_REFUND".equals(workType),
            "PENDING_RETURN".equals(workType),
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

    private PageResponse<AdminMemberResponse> toMemberPage(
        Page<Member> members,
        boolean revealSensitiveData
    ) {
        List<Long> memberIds = members.stream().map(Member::getId).toList();
        Map<Long, Long> orderCounts = new HashMap<>();
        Map<Long, BigDecimal> paidAmounts = new HashMap<>();
        if (!memberIds.isEmpty()) {
            orderRepository.countOrdersByMemberIds(memberIds).forEach(row ->
                orderCounts.put((Long) row[0], (Long) row[1]));
            paymentRepository.sumApprovedAmountByMemberIds(memberIds, PaymentResult.SUCCESS)
                .forEach(row -> paidAmounts.put((Long) row[0], (BigDecimal) row[1]));
        }
        return PageResponse.from(members.map(member -> toMemberResponse(
            member,
            revealSensitiveData,
            orderCounts.getOrDefault(member.getId(), 0L),
            paidAmounts.getOrDefault(member.getId(), BigDecimal.ZERO)
        )));
    }

    private AdminMemberResponse toMemberResponse(
        Member member,
        boolean revealSensitiveData,
        long orderCount,
        BigDecimal totalPaidAmount
    ) {
        return new AdminMemberResponse(
            member.getId(),
            revealSensitiveData ? member.getEmail() : maskEmail(member.getEmail()),
            member.getName(),
            member.getRole(),
            member.getAdminGrade(),
            member.getAccessStatus(),
            member.getLastLoginAt(),
            member.getRestrictionReason(),
            member.getRestrictedAt(),
            member.getRestrictedBy() == null ? null : member.getRestrictedBy().getId(),
            orderCount,
            totalPaidAmount,
            member.getCreatedAt()
        );
    }

    private AdminMemberResponse toMemberResponseWithMetrics(
        Member member,
        boolean revealSensitiveData
    ) {
        Long memberId = member.getId();
        long orderCount = orderRepository.countOrdersByMemberIds(List.of(memberId)).stream()
            .findFirst()
            .map(row -> (Long) row[1])
            .orElse(0L);
        BigDecimal totalPaidAmount = paymentRepository
            .sumApprovedAmountByMemberIds(List.of(memberId), PaymentResult.SUCCESS).stream()
            .findFirst()
            .map(row -> (BigDecimal) row[1])
            .orElse(BigDecimal.ZERO);
        return toMemberResponse(member, revealSensitiveData, orderCount, totalPaidAmount);
    }

    private AdminAuditLogResponse toAuditLogResponse(AdminAuditLog log) {
        return new AdminAuditLogResponse(
            log.getId(),
            log.getActor().getId(),
            log.getActor().getName(),
            log.getActorGrade(),
            log.getAction(),
            log.getBeforeValue(),
            log.getAfterValue(),
            log.getReason(),
            log.getCreatedAt()
        );
    }

    private Member requireAdmin(Long actorMemberId) {
        Member actor = memberRepository.findById(actorMemberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (actor.getRole() != MemberRole.ROLE_ADMIN || actor.getAdminGrade() == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return actor;
    }

    private void validateMemberOperation(Member actor, Member target) {
        if (actor.getRole() != MemberRole.ROLE_ADMIN || actor.getAdminGrade() == null
            || actor.getId().equals(target.getId())) {
            throw new BusinessException(ErrorCode.MEMBER_OPERATION_FORBIDDEN);
        }
        AdminGrade actorGrade = actor.getAdminGrade();
        if (!actorGrade.hasPermission(AdminPermission.MEMBER_RESTRICT_LIMITED)
            && !actorGrade.hasPermission(AdminPermission.MEMBER_RESTRICT_ALL)) {
            throw new BusinessException(ErrorCode.MEMBER_OPERATION_FORBIDDEN);
        }
        if (!actorGrade.hasPermission(AdminPermission.MEMBER_RESTRICT_ALL)
            && target.getRole() != MemberRole.ROLE_USER) {
            throw new BusinessException(ErrorCode.MEMBER_OPERATION_FORBIDDEN);
        }
        if (target.getRole() == MemberRole.ROLE_ADMIN) {
            if (target.getAdminGrade() == null
                || target.getAdminGrade().level() >= actorGrade.level()) {
                throw new BusinessException(ErrorCode.MEMBER_OPERATION_FORBIDDEN);
            }
        }
    }

    private boolean canReadSensitiveMemberData(Member actor) {
        return actor.getAdminGrade() == AdminGrade.SUPER_ADMIN;
    }

    private String maskEmail(String email) {
        int separator = email.indexOf('@');
        if (separator <= 1) return "***" + email.substring(Math.max(separator, 0));
        return email.substring(0, 1) + "***" + email.substring(separator);
    }

    private AdminSellerResponse toSellerListResponse(
        com.ymall.backend.seller.entity.SellerProfile profile,
        boolean revealSensitiveData
    ) {
        return new AdminSellerResponse(
            profile.getId(),
            profile.getMember().getId(),
            revealSensitiveData ? profile.getMember().getEmail() : maskEmail(profile.getMember().getEmail()),
            profile.getMember().getName(),
            profile.getStoreName(),
            revealSensitiveData ? profile.getBusinessNumber() : maskBusinessNumber(profile.getBusinessNumber()),
            0L, 0L, 0L, BigDecimal.ZERO, 0L, 0L, 0L, 0L,
            null, null, null,
            profile.getCreatedAt()
        );
    }

    private AdminSellerResponse toSellerDetailResponse(
        com.ymall.backend.seller.entity.SellerProfile profile,
        boolean revealSensitiveData
    ) {
        Long sellerProfileId = profile.getId();
        List<OrderStatus> paidStatuses = List.of(
            OrderStatus.PAID,
            OrderStatus.PREPARING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED,
            OrderStatus.PARTIALLY_REFUNDED,
            OrderStatus.REFUNDED
        );
        SellerApplication application = sellerApplicationRepository
            .findByMemberId(profile.getMember().getId())
            .orElse(null);
        return new AdminSellerResponse(
            sellerProfileId,
            profile.getMember().getId(),
            revealSensitiveData ? profile.getMember().getEmail() : maskEmail(profile.getMember().getEmail()),
            profile.getMember().getName(),
            profile.getStoreName(),
            revealSensitiveData ? profile.getBusinessNumber() : maskBusinessNumber(profile.getBusinessNumber()),
            productRepository.countBySellerProfileIdAndStatusNot(sellerProfileId, ProductStatus.DELETED),
            productRepository.countBySellerProfileIdAndStatus(sellerProfileId, ProductStatus.PENDING),
            orderItemRepository.countSellerOrders(sellerProfileId, paidStatuses),
            orderItemRepository.sumSellerGrossSales(sellerProfileId, paidStatuses),
            orderItemRepository.sumSellerRefundedQuantity(sellerProfileId),
            returnRequestRepository.countBySellerProfileIdAndStatus(
                sellerProfileId,
                ReturnRequestStatus.REQUESTED
            ),
            supportInquiryRepository.countByMemberIdAndStatusIn(
                profile.getMember().getId(),
                List.of(
                    SupportInquiryStatus.WAITING,
                    SupportInquiryStatus.IN_PROGRESS,
                    SupportInquiryStatus.LIVE_REQUESTED,
                    SupportInquiryStatus.LIVE_OFFERED,
                    SupportInquiryStatus.LIVE_ACTIVE
                )
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

    private String maskBusinessNumber(String businessNumber) {
        if (businessNumber.length() <= 5) return "***";
        return businessNumber.substring(0, 3) + "-**-" + businessNumber.substring(businessNumber.length() - 4);
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
