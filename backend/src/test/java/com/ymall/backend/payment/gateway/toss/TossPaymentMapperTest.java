package com.ymall.backend.payment.gateway.toss;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;

class TossPaymentMapperTest {

    private final TossPaymentMapper mapper = new TossPaymentMapper();

    @Test
    void mapsTossPaymentResponseToGatewayResult() {
        OffsetDateTime approvedAt = OffsetDateTime.parse("2026-07-22T20:00:00+09:00");
        TossPaymentResponse response = new TossPaymentResponse(
            "payment-key",
            "order-123456",
            "DONE",
            BigDecimal.valueOf(39_000),
            BigDecimal.valueOf(39_000),
            "카드",
            approvedAt
        );

        PaymentGatewayResult result = mapper.toResult(response);

        assertThat(result.paymentKey()).isEqualTo("payment-key");
        assertThat(result.orderId()).isEqualTo("order-123456");
        assertThat(result.status()).isEqualTo(PaymentGatewayStatus.DONE);
        assertThat(result.totalAmount()).isEqualByComparingTo("39000");
        assertThat(result.approvedAt()).isEqualTo(approvedAt);
    }

    @Test
    void mapsUnknownProviderStatusToUnknown() {
        TossPaymentResponse response = new TossPaymentResponse(
            "payment-key",
            "order-123456",
            "NEW_PROVIDER_STATUS",
            BigDecimal.ONE,
            BigDecimal.ONE,
            null,
            null
        );

        PaymentGatewayResult result = mapper.toResult(response);

        assertThat(result.status()).isEqualTo(PaymentGatewayStatus.UNKNOWN);
    }
}
