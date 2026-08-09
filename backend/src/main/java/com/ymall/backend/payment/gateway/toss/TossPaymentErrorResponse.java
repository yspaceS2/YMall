package com.ymall.backend.payment.gateway.toss;

record TossPaymentErrorResponse(
    String code,
    String message
) {
}
