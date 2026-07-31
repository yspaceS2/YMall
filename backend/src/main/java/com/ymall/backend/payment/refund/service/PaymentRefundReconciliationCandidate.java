package com.ymall.backend.payment.refund.service;

record PaymentRefundReconciliationCandidate(
    Long refundId,
    String paymentKey
) {
}
