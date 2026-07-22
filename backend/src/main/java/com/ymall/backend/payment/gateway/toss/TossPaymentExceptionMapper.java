package com.ymall.backend.payment.gateway.toss;

import java.util.Set;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.payment.exception.PaymentException;

@Component
class TossPaymentExceptionMapper {

    private static final Set<String> CREDENTIAL_ERROR_CODES = Set.of(
        "INVALID_API_KEY",
        "UNAUTHORIZED_KEY"
    );

    PaymentException map(HttpStatusCode status, TossPaymentErrorResponse response) {
        String providerCode = response == null ? null : response.code();
        String providerMessage = response == null ? null : response.message();

        if (providerCode != null && CREDENTIAL_ERROR_CODES.contains(providerCode)) {
            return new PaymentException(
                ErrorCode.PAYMENT_GATEWAY_CONFIGURATION_ERROR,
                providerCode,
                providerMessage
            );
        }
        if (status.is5xxServerError()) {
            return new PaymentException(
                ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE,
                providerCode,
                providerMessage
            );
        }
        return new PaymentException(
            ErrorCode.PAYMENT_GATEWAY_ERROR,
            providerCode,
            providerMessage
        );
    }
}
