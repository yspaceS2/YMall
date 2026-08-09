package com.ymall.backend.settlement.service;

import org.springframework.stereotype.Component;

import com.ymall.backend.settlement.dto.SettlementRequestHistoryResponse;
import com.ymall.backend.settlement.dto.SettlementRequestResponse;
import com.ymall.backend.settlement.entity.SettlementRequest;
import com.ymall.backend.settlement.entity.SettlementRequestHistory;

@Component
class SettlementRequestResponseMapper {

    SettlementRequestResponse toResponse(SettlementRequest request) {
        return new SettlementRequestResponse(
            request.getId(),
            request.getSellerProfile().getId(),
            request.getSellerProfile().getStoreName(),
            request.getPeriodStart(),
            request.getPeriodEnd(),
            request.getStatus(),
            request.getGrossAmount(),
            request.getFeeAmount(),
            request.getSettlementAmount(),
            request.getRejectionReason(),
            request.getMockPaymentReference(),
            request.getReviewedAt(),
            request.getPaidAt(),
            request.getCreatedAt(),
            request.getUpdatedAt()
        );
    }

    SettlementRequestHistoryResponse toHistoryResponse(
        SettlementRequestHistory history
    ) {
        return new SettlementRequestHistoryResponse(
            history.getFromStatus(),
            history.getToStatus(),
            history.getActor().getId(),
            history.getActor().getName(),
            history.getReason(),
            history.getCreatedAt()
        );
    }
}
