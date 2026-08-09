package com.ymall.backend.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.payment.dto.MockPaymentRequest;
import com.ymall.backend.payment.dto.PaymentConfirmRequest;
import com.ymall.backend.payment.dto.PaymentResponse;
import com.ymall.backend.payment.service.PaymentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders/{orderId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId,
        @Valid @RequestBody MockPaymentRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                paymentService.processPayment(principal.memberId(), orderId, request),
                "모의 결제를 처리했습니다."
            ));
    }

    @PostMapping("/confirmations")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId,
        @Valid @RequestBody PaymentConfirmRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                paymentService.confirmPayment(principal.memberId(), orderId, request),
                "결제가 승인되었습니다."
            ));
    }
}
