package com.ymall.backend.order.returnrequest.service;

public record ReturnApprovalCommand(
    Long returnRequestId,
    Long orderId,
    Long orderItemId,
    int quantity,
    String reason
) {
}
