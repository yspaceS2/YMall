package com.ymall.backend.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.order.dto.OrderCreateRequest;
import com.ymall.backend.order.dto.OrderResponse;
import com.ymall.backend.order.service.OrderService;
import com.ymall.backend.payment.refund.dto.PaymentRefundRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;
import com.ymall.backend.payment.refund.service.PaymentRefundService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentRefundService paymentRefundService;

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getOrders(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(orderService.getOrders(principal.memberId(), page, size));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId
    ) {
        return ApiResponse.success(orderService.getOrder(principal.memberId(), orderId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody OrderCreateRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                orderService.createOrder(principal.memberId(), request),
                "주문을 생성했습니다."
            ));
    }

    @PostMapping("/{orderId}/cancellations")
    public ApiResponse<OrderResponse> cancelOrder(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId
    ) {
        return ApiResponse.success(
            orderService.cancelOrder(principal.memberId(), orderId),
            "주문을 취소했습니다."
        );
    }

    @PostMapping("/{orderId}/refunds")
    public ApiResponse<PaymentRefundResponse> refundOrder(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId,
        @Valid @RequestBody PaymentRefundRequest request
    ) {
        return ApiResponse.success(
            paymentRefundService.refundUser(
                principal.memberId(),
                orderId,
                request
            ),
            "환불 요청을 처리했습니다."
        );
    }

    @GetMapping("/{orderId}/refunds")
    public ApiResponse<java.util.List<PaymentRefundResponse>> getRefunds(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId
    ) {
        return ApiResponse.success(
            paymentRefundService.getRefunds(principal.memberId(), orderId)
        );
    }
}
