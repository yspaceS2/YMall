package com.ymall.backend.payment.gateway;

public interface PaymentGateway {

    PaymentGatewayResult confirm(PaymentConfirmCommand command);

    PaymentGatewayResult findByPaymentKey(String paymentKey);

    PaymentGatewayResult cancel(PaymentCancelCommand command);
}
