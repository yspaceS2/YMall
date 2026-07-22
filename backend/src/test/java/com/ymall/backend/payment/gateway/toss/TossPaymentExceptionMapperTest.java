package com.ymall.backend.payment.gateway.toss;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.payment.exception.PaymentException;

class TossPaymentExceptionMapperTest {

    private final TossPaymentExceptionMapper mapper = new TossPaymentExceptionMapper();

    @Test
    void mapsInvalidApiKeyToConfigurationError() {
        PaymentException exception = mapper.map(
            HttpStatus.UNAUTHORIZED,
            new TossPaymentErrorResponse("INVALID_API_KEY", "잘못된 API 키입니다.")
        );

        assertThat(exception.getErrorCode())
            .isEqualTo(ErrorCode.PAYMENT_GATEWAY_CONFIGURATION_ERROR);
        assertThat(exception.getProviderCode()).isEqualTo("INVALID_API_KEY");
    }

    @Test
    void mapsProviderServerErrorToUnavailableError() {
        PaymentException exception = mapper.map(
            HttpStatus.INTERNAL_SERVER_ERROR,
            new TossPaymentErrorResponse("PROVIDER_ERROR", "일시적인 오류입니다.")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE);
        assertThat(exception.getProviderCode()).isEqualTo("PROVIDER_ERROR");
    }

    @Test
    void mapsPaymentRejectionToGatewayError() {
        PaymentException exception = mapper.map(
            HttpStatus.BAD_REQUEST,
            new TossPaymentErrorResponse("REJECT_CARD_PAYMENT", "카드 결제가 거절됐습니다.")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_GATEWAY_ERROR);
        assertThat(exception.getProviderCode()).isEqualTo("REJECT_CARD_PAYMENT");
    }
}
