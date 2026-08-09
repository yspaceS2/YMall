package com.ymall.backend.order.returnrequest.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.order.returnrequest.dto.ReturnRequestCreateRequest;
import com.ymall.backend.order.returnrequest.dto.ReturnRequestResponse;
import com.ymall.backend.order.returnrequest.service.ReturnRequestService;

@RestController
@RequestMapping("/api/orders/{orderId}/return-requests")
@RequiredArgsConstructor
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> create(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId,
        @Valid @RequestBody ReturnRequestCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
            returnRequestService.create(principal.memberId(), orderId, request),
            "반품을 신청했습니다."
        ));
    }

    @GetMapping
    public ApiResponse<List<ReturnRequestResponse>> getRequests(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long orderId
    ) {
        return ApiResponse.success(
            returnRequestService.getMemberRequests(principal.memberId(), orderId)
        );
    }
}
