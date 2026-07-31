package com.ymall.backend.seller.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.seller.dto.SellerOrderDetailResponse;
import com.ymall.backend.seller.dto.SellerOrderItemFulfillmentUpdateRequest;
import com.ymall.backend.seller.dto.SellerOrderResponse;
import com.ymall.backend.seller.dto.SellerOrderStatusUpdateRequest;
import com.ymall.backend.seller.dto.SellerPendingOrderCountResponse;
import com.ymall.backend.seller.service.SellerOrderService;
import com.ymall.backend.payment.refund.dto.PaymentRefundRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;
import com.ymall.backend.payment.refund.service.PaymentRefundService;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;
    private final PaymentRefundService paymentRefundService;

    @GetMapping("/pending-count")
    public ApiResponse<SellerPendingOrderCountResponse> getPendingOrderCount(
        @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ApiResponse.success(
            sellerOrderService.getPendingOrderCount(principal.memberId())
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SellerOrderResponse>>> getOrders(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "") String keyword,
        @RequestParam(required = false) OrderItemFulfillmentStatus fulfillmentStatus
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            sellerOrderService.getOrders(
                principal.memberId(),
                page,
                size,
                keyword,
                fulfillmentStatus
            )
        ));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<SellerOrderDetailResponse> getOrder(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId
    ) {
        return ApiResponse.success(
            sellerOrderService.getOrder(principal.memberId(), orderId)
        );
    }

    @PatchMapping("/{orderId}/items/{orderItemId}/fulfillment")
    public ApiResponse<SellerOrderDetailResponse> updateItemFulfillment(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId,
        @PathVariable Long orderItemId,
        @Valid @RequestBody SellerOrderItemFulfillmentUpdateRequest request
    ) {
        return ApiResponse.success(
            sellerOrderService.updateItemStatus(
                principal.memberId(),
                orderId,
                orderItemId,
                request
            )
        );
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<SellerOrderResponse>> updateStatus(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId,
        @Valid @RequestBody SellerOrderStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            sellerOrderService.updateStatus(principal.memberId(), orderId, request)
        ));
    }

    @PostMapping("/{orderId}/refunds")
    public ApiResponse<PaymentRefundResponse> refundOrder(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId,
        @Valid @RequestBody PaymentRefundRequest request
    ) {
        return ApiResponse.success(
            paymentRefundService.refundSeller(principal.memberId(), orderId, request),
            "환불 요청을 처리했습니다."
        );
    }

    @GetMapping("/{orderId}/refunds")
    public ApiResponse<java.util.List<PaymentRefundResponse>> getRefunds(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId
    ) {
        return ApiResponse.success(
            paymentRefundService.getSellerRefunds(principal.memberId(), orderId)
        );
    }
}
