package com.ymall.backend.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ymall.backend.support.entity.SupportInquiryCategory;

public record SupportInquiryCreateRequest(
    @NotNull SupportInquiryCategory category,
    @NotBlank @Size(max = 120) String title,
    @NotBlank @Size(max = 2000) String content,
    Long relatedOrderId,
    Long relatedProductId,
    Long relatedSettlementId
) {
}
