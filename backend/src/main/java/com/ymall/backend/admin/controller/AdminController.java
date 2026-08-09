package com.ymall.backend.admin.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.admin.dto.AdminMemberResponse;
import com.ymall.backend.admin.dto.AdminAuditLogResponse;
import com.ymall.backend.admin.dto.AdminMemberRestrictionRequest;
import com.ymall.backend.admin.dto.AdminSessionRevokeRequest;
import com.ymall.backend.admin.entity.AdminGrade;
import com.ymall.backend.admin.dto.AdminOrderResponse;
import com.ymall.backend.admin.dto.AdminProductResponse;
import com.ymall.backend.admin.dto.AdminProductStatusUpdateRequest;
import com.ymall.backend.admin.dto.AdminProductChangeStatusUpdateRequest;
import com.ymall.backend.admin.dto.AdminSellerResponse;
import com.ymall.backend.admin.service.AdminService;
import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.dto.ProductChangeRequestResponse;
import com.ymall.backend.product.service.ProductChangeReviewService;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.member.entity.MemberAccessStatus;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.payment.refund.dto.PaymentRefundRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;
import com.ymall.backend.payment.refund.service.PaymentRefundService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final PaymentRefundService paymentRefundService;
    private final ProductChangeReviewService productChangeReviewService;

    @GetMapping("/products")
    public ApiResponse<PageResponse<AdminProductResponse>> getProducts(
        @RequestParam(defaultValue = "PENDING") ProductStatus status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ApiResponse.success(adminService.getProducts(status, page, size, keyword));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<AdminProductResponse> getProduct(
        @PathVariable Long productId
    ) {
        return ApiResponse.success(adminService.getProduct(productId));
    }

    @PatchMapping("/products/{productId}/status")
    public ApiResponse<AdminProductResponse> updateProductStatus(
        @PathVariable Long productId,
        @Valid @RequestBody AdminProductStatusUpdateRequest request
    ) {
        return ApiResponse.success(
            adminService.updateProductStatus(productId, request),
            "상품 승인 상태를 변경했습니다."
        );
    }

    @GetMapping("/product-change-requests")
    public ApiResponse<PageResponse<ProductChangeRequestResponse>> getProductChangeRequests(
        @RequestParam(defaultValue = "PENDING") ProductStatus status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
            productChangeReviewService.getRequests(status, page, size)
        );
    }

    @GetMapping("/product-change-requests/{requestId}")
    public ApiResponse<ProductChangeRequestResponse> getProductChangeRequest(
        @PathVariable Long requestId
    ) {
        return ApiResponse.success(productChangeReviewService.getRequest(requestId));
    }

    @PatchMapping("/product-change-requests/{requestId}/status")
    public ApiResponse<ProductChangeRequestResponse> reviewProductChangeRequest(
        @PathVariable Long requestId,
        @Valid @RequestBody AdminProductChangeStatusUpdateRequest request
    ) {
        return ApiResponse.success(
            productChangeReviewService.review(requestId, request),
            "상품 변경 심사를 처리했습니다."
        );
    }

    @GetMapping("/members")
    public ApiResponse<PageResponse<AdminMemberResponse>> getMembers(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(required = false) MemberAccessStatus status,
        @RequestParam(required = false) MemberRole role,
        @RequestParam(required = false) AdminGrade adminGrade,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate joinedFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate joinedTo
    ) {
        return ApiResponse.success(adminService.getMembers(
            principal.memberId(), page, size, keyword, status, role, adminGrade,
            joinedFrom, joinedTo
        ));
    }

    @GetMapping("/members/{memberId}")
    public ApiResponse<AdminMemberResponse> getMember(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long memberId
    ) {
        return ApiResponse.success(adminService.getMember(principal.memberId(), memberId));
    }

    @PatchMapping("/members/{memberId}/restriction")
    public ApiResponse<AdminMemberResponse> changeMemberRestriction(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long memberId,
        @Valid @RequestBody AdminMemberRestrictionRequest request
    ) {
        return ApiResponse.success(
            adminService.changeMemberRestriction(principal.memberId(), memberId, request),
            request.restricted() ? "회원 이용을 제한했습니다." : "회원 이용 제한을 해제했습니다."
        );
    }

    @PostMapping("/members/{memberId}/sessions/revoke")
    public ApiResponse<Void> revokeMemberSessions(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long memberId,
        @Valid @RequestBody AdminSessionRevokeRequest request
    ) {
        adminService.revokeMemberSessions(principal.memberId(), memberId, request);
        return ApiResponse.success(null, "회원의 모든 로그인 세션을 종료했습니다.");
    }

    @GetMapping("/members/{memberId}/audit-logs")
    public ApiResponse<List<AdminAuditLogResponse>> getMemberAuditLogs(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long memberId
    ) {
        return ApiResponse.success(adminService.getMemberAuditLogs(
            principal.memberId(), memberId
        ));
    }

    @GetMapping("/sellers")
    public ApiResponse<PageResponse<AdminSellerResponse>> getSellers(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ApiResponse.success(adminService.getSellers(
            principal.memberId(), page, size, keyword
        ));
    }

    @GetMapping("/sellers/{sellerId}")
    public ApiResponse<AdminSellerResponse> getSeller(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long sellerId
    ) {
        return ApiResponse.success(adminService.getSeller(principal.memberId(), sellerId));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<AdminOrderResponse>> getOrders(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(defaultValue = "") String workType
    ) {
        return ApiResponse.success(adminService.getOrders(page, size, keyword, workType));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<AdminOrderResponse> getOrder(@PathVariable Long orderId) {
        return ApiResponse.success(adminService.getOrder(orderId));
    }

    @PostMapping("/orders/{orderId}/refunds")
    public ApiResponse<PaymentRefundResponse> refundOrder(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId,
        @Valid @RequestBody PaymentRefundRequest request
    ) {
        return ApiResponse.success(
            paymentRefundService.refundAdmin(principal.memberId(), orderId, request),
            "환불 요청을 처리했습니다."
        );
    }

    @GetMapping("/orders/{orderId}/refunds")
    public ApiResponse<java.util.List<PaymentRefundResponse>> getRefunds(
        @PathVariable Long orderId
    ) {
        return ApiResponse.success(paymentRefundService.getAdminRefunds(orderId));
    }
}
