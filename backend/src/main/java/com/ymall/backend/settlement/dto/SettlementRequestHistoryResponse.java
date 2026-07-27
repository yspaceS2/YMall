package com.ymall.backend.settlement.dto;

import java.time.Instant;

import com.ymall.backend.settlement.entity.SettlementRequestStatus;

public record SettlementRequestHistoryResponse(
    SettlementRequestStatus fromStatus,
    SettlementRequestStatus toStatus,
    Long actorMemberId,
    String actorName,
    String reason,
    Instant createdAt
) {
}
