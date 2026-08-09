package com.ymall.backend.payment.gateway.toss;

import org.springframework.stereotype.Component;

import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;

@Component
class TossPaymentMapper {

    PaymentGatewayResult toResult(TossPaymentResponse response) {
        return new PaymentGatewayResult(
            response.paymentKey(),
            response.orderId(),
            toStatus(response.status()),
            response.totalAmount(),
            response.balanceAmount(),
            response.method(),
            response.approvedAt()
        );
    }

    private PaymentGatewayStatus toStatus(String status) {
        if (status == null) {
            return PaymentGatewayStatus.UNKNOWN;
        }

        try {
            return PaymentGatewayStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            return PaymentGatewayStatus.UNKNOWN;
        }
    }
}
