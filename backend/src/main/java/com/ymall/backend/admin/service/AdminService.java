package com.ymall.backend.admin.service;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.ymall.backend.admin.dto.AdminAuditLogResponse;
import com.ymall.backend.admin.dto.AdminMemberResponse;
import com.ymall.backend.admin.dto.AdminMemberRestrictionRequest;
import com.ymall.backend.admin.dto.AdminOrderResponse;
import com.ymall.backend.admin.dto.AdminProductResponse;
import com.ymall.backend.admin.dto.AdminProductStatusUpdateRequest;
import com.ymall.backend.admin.dto.AdminSellerResponse;
import com.ymall.backend.admin.dto.AdminSessionRevokeRequest;
import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.member.entity.MemberAccessStatus;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.product.entity.ProductStatus;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminProductManagementService productManagementService;
    private final AdminMemberManagementService memberManagementService;
    private final AdminSellerManagementService sellerManagementService;
    private final AdminOrderManagementService orderManagementService;

    public PageResponse<AdminProductResponse> getProducts(
        ProductStatus status,
        int page,
        int size,
        String keyword
    ) {
        return productManagementService.getProducts(status, page, size, keyword);
    }

    public AdminProductResponse getProduct(Long productId) {
        return productManagementService.getProduct(productId);
    }

    public AdminProductResponse updateProductStatus(
        Long productId,
        AdminProductStatusUpdateRequest request
    ) {
        return productManagementService.updateProductStatus(productId, request);
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
        return memberManagementService.getMembers(
            actorMemberId,
            page,
            size,
            keyword,
            accessStatus,
            role,
            adminGrade,
            joinedFrom,
            joinedTo
        );
    }

    public AdminMemberResponse getMember(Long actorMemberId, Long memberId) {
        return memberManagementService.getMember(actorMemberId, memberId);
    }

    public AdminMemberResponse changeMemberRestriction(
        Long actorMemberId,
        Long memberId,
        AdminMemberRestrictionRequest request
    ) {
        return memberManagementService.changeMemberRestriction(
            actorMemberId,
            memberId,
            request
        );
    }

    public void revokeMemberSessions(
        Long actorMemberId,
        Long memberId,
        AdminSessionRevokeRequest request
    ) {
        memberManagementService.revokeMemberSessions(actorMemberId, memberId, request);
    }

    public List<AdminAuditLogResponse> getMemberAuditLogs(
        Long actorMemberId,
        Long memberId
    ) {
        return memberManagementService.getMemberAuditLogs(actorMemberId, memberId);
    }

    public PageResponse<AdminSellerResponse> getSellers(
        Long actorMemberId,
        int page,
        int size,
        String keyword
    ) {
        return sellerManagementService.getSellers(actorMemberId, page, size, keyword);
    }

    public AdminSellerResponse getSeller(Long actorMemberId, Long sellerId) {
        return sellerManagementService.getSeller(actorMemberId, sellerId);
    }

    public PageResponse<AdminOrderResponse> getOrders(
        int page,
        int size,
        String keyword,
        String workType
    ) {
        return orderManagementService.getOrders(page, size, keyword, workType);
    }

    public AdminOrderResponse getOrder(Long orderId) {
        return orderManagementService.getOrder(orderId);
    }
}
