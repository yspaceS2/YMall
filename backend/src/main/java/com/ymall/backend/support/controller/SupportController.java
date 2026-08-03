package com.ymall.backend.support.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import com.ymall.backend.global.common.ApiResponse;
import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.security.MemberPrincipal;
import com.ymall.backend.support.dto.SupportInquiryCreateRequest;
import com.ymall.backend.support.dto.SupportInquiryDetailResponse;
import com.ymall.backend.support.dto.SupportInquirySummaryResponse;
import com.ymall.backend.support.dto.SupportMessageCreateRequest;
import com.ymall.backend.support.dto.SupportMessageResponse;
import com.ymall.backend.support.service.SupportService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/support/inquiries")
public class SupportController {

    private final SupportService supportService;

    @GetMapping
    public ApiResponse<PageResponse<SupportInquirySummaryResponse>> getMyInquiries(
        @AuthenticationPrincipal MemberPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(supportService.getMyInquiries(principal.memberId(), page, size));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupportInquiryDetailResponse>> create(
        @AuthenticationPrincipal MemberPrincipal principal,
        @Valid @RequestBody SupportInquiryCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
            supportService.create(principal, request),
            "문의가 등록되었습니다."
        ));
    }

    @GetMapping("/{inquiryId}")
    public ApiResponse<SupportInquiryDetailResponse> getInquiry(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.success(supportService.getInquiry(principal, inquiryId));
    }

    @PostMapping(value = "/{inquiryId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<SupportMessageResponse> addMessage(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long inquiryId,
        @Valid @RequestBody SupportMessageCreateRequest request
    ) {
        return ApiResponse.success(
            supportService.addMessage(principal, inquiryId, request, false),
            "추가 문의가 등록되었습니다."
        );
    }

    @PostMapping(value = "/{inquiryId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SupportMessageResponse> addMessageWithAttachments(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long inquiryId,
        @RequestParam UUID clientMessageId,
        @RequestParam(defaultValue = "") String content,
        @RequestParam("files") List<MultipartFile> files
    ) {
        return ApiResponse.success(
            supportService.addMessageWithAttachments(
                principal,
                inquiryId,
                clientMessageId,
                content,
                files
            ),
            "첨부파일이 등록되었습니다."
        );
    }

    @PostMapping("/{inquiryId}/live-requests")
    public ApiResponse<SupportInquiryDetailResponse> requestLive(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.success(
            supportService.requestLive(principal, inquiryId),
            "실시간 상담을 요청했습니다."
        );
    }

    @PostMapping("/{inquiryId}/live-requests/accept")
    public ApiResponse<SupportInquiryDetailResponse> acceptLive(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.success(supportService.acceptLive(principal, inquiryId));
    }

    @PostMapping("/{inquiryId}/live-requests/reject")
    public ApiResponse<SupportInquiryDetailResponse> rejectLive(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.success(supportService.rejectLive(principal, inquiryId));
    }

    @PostMapping("/{inquiryId}/live-requests/cancel")
    public ApiResponse<SupportInquiryDetailResponse> cancelLive(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.success(supportService.cancelLive(principal, inquiryId));
    }

    @PostMapping("/{inquiryId}/live-requests/end")
    public ApiResponse<SupportInquiryDetailResponse> endLive(
        @AuthenticationPrincipal MemberPrincipal principal,
        @PathVariable Long inquiryId
    ) {
        return ApiResponse.success(supportService.endLive(principal, inquiryId));
    }

}
