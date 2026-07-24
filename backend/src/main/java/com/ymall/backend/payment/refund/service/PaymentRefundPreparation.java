package com.ymall.backend.payment.refund.service;

import java.math.BigDecimal;

import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;

record PaymentRefundPreparation(
    Long refundId,
    String paymentKey,
    BigDecimal amount,
    String reason,
    String idempotencyKey,
    PaymentRefundResponse existingResponse
) {
    static PaymentRefundPreparation existing(PaymentRefundResponse response) {
        return new PaymentRefundPreparation(null, null, null, null, null, response);
    }

    boolean requiresGatewayCall() {
        return existingResponse == null;
    }
}
