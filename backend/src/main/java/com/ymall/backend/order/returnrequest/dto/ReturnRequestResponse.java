package com.ymall.backend.order.returnrequest.dto;

import java.time.LocalDateTime;

import com.ymall.backend.order.returnrequest.entity.ReturnRequestStatus;

public record ReturnRequestResponse(
    Long returnRequestId,
    Long orderId,
    Long orderItemId,
    Long productId,
    String productName,
    String thumbnailUrl,
    String memberName,
    int quantity,
    String reason,
    ReturnRequestStatus status,
    String sellerResponse,
    Long paymentRefundId,
    LocalDateTime returnDeadline,
    LocalDateTime requestedAt,
    LocalDateTime processedAt
) {
}
