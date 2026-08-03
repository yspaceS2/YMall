package com.ymall.backend.support.dto;

import java.util.List;

public record SupportInquiryDetailResponse(
    SupportInquirySummaryResponse inquiry,
    Long relatedOrderId,
    Long relatedProductId,
    Long relatedSettlementId,
    SupportChatSessionResponse chatSession,
    List<SupportMessageResponse> messages
) {
}
