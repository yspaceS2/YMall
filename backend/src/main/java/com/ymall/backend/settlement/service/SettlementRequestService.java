package com.ymall.backend.settlement.service;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.settlement.dto.SettlementAvailabilityResponse;
import com.ymall.backend.settlement.dto.SettlementRequestHistoryResponse;
import com.ymall.backend.settlement.dto.SettlementRequestResponse;
import com.ymall.backend.settlement.dto.SettlementRequestWorkType;
import com.ymall.backend.settlement.entity.SettlementRequestStatus;

@Service
@RequiredArgsConstructor
public class SettlementRequestService {

    private final SettlementRequestQueryService queryService;
    private final SettlementRequestCommandService commandService;

    public SettlementAvailabilityResponse getAvailability(Long memberId) {
        return queryService.getAvailability(memberId);
    }

    public PageResponse<SettlementRequestResponse> getSellerRequests(
        Long memberId,
        SettlementRequestStatus status,
        SettlementRequestWorkType workType,
        Long requestId,
        LocalDate requestedFrom,
        LocalDate requestedTo,
        int page,
        int size
    ) {
        return queryService.getSellerRequests(
            memberId,
            status,
            workType,
            requestId,
            requestedFrom,
            requestedTo,
            page,
            size
        );
    }

    public SettlementRequestResponse getSellerRequest(Long memberId, Long requestId) {
        return queryService.getSellerRequest(memberId, requestId);
    }

    public SettlementRequestResponse request(Long memberId) {
        return commandService.request(memberId);
    }

    public PageResponse<SettlementRequestResponse> getAdminRequests(
        SettlementRequestStatus status,
        SettlementRequestWorkType workType,
        Long requestId,
        String sellerKeyword,
        LocalDate requestedFrom,
        LocalDate requestedTo,
        int page,
        int size
    ) {
        return queryService.getAdminRequests(
            status,
            workType,
            requestId,
            sellerKeyword,
            requestedFrom,
            requestedTo,
            page,
            size
        );
    }

    public SettlementRequestResponse getAdminRequest(Long requestId) {
        return queryService.getAdminRequest(requestId);
    }

    public SettlementRequestResponse approve(Long adminMemberId, Long requestId) {
        return commandService.approve(adminMemberId, requestId);
    }

    public SettlementRequestResponse reject(
        Long adminMemberId,
        Long requestId,
        String reason
    ) {
        return commandService.reject(adminMemberId, requestId, reason);
    }

    public SettlementRequestResponse completeMockPayment(
        Long adminMemberId,
        Long requestId
    ) {
        return commandService.completeMockPayment(adminMemberId, requestId);
    }

    public List<SettlementRequestHistoryResponse> getHistory(Long requestId) {
        return queryService.getHistory(requestId);
    }
}
