package com.ymall.backend.payment.gateway.toss;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.payment.exception.PaymentException;
import com.ymall.backend.payment.gateway.PaymentConfirmCommand;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;

class TossPaymentAdapterTest {

    private MockRestServiceServer server;
    private TossPaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
            .baseUrl("https://api.tosspayments.com");
        server = MockRestServiceServer.bindTo(builder).build();

        TossPaymentProperties properties = new TossPaymentProperties();
        properties.setClientKey("test_ck_ymall");
        properties.setSecretKey("test_sk_ymall");
        adapter = new TossPaymentAdapter(
            builder.build(),
            new ObjectMapper(),
            properties,
            new TossPaymentMapper(),
            new TossPaymentExceptionMapper()
        );
    }

    @Test
    void confirmsPaymentAndMapsResponse() {
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Idempotency-Key", "idempotency-key"))
            .andExpect(jsonPath("$.paymentKey").value("payment-key"))
            .andExpect(jsonPath("$.orderId").value("order-123456"))
            .andExpect(jsonPath("$.amount").value(39_000))
            .andRespond(withSuccess(
                """
                    {
                      "paymentKey": "payment-key",
                      "orderId": "order-123456",
                      "status": "DONE",
                      "totalAmount": 39000,
                      "balanceAmount": 39000,
                      "method": "카드",
                      "approvedAt": null
                    }
                    """,
                MediaType.APPLICATION_JSON
            ));

        PaymentGatewayResult result = adapter.confirm(new PaymentConfirmCommand(
            "payment-key",
            "order-123456",
            BigDecimal.valueOf(39_000),
            "idempotency-key"
        ));

        assertThat(result.status()).isEqualTo(PaymentGatewayStatus.DONE);
        assertThat(result.totalAmount()).isEqualByComparingTo("39000");
        server.verify();
    }

    @Test
    void convertsProviderErrorToPaymentException() {
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/payment-key"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withUnauthorizedRequest().body(
                """
                    {
                      "code": "INVALID_API_KEY",
                      "message": "잘못된 API 키입니다."
                    }
                    """
            ).contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.findByPaymentKey("payment-key"))
            .isInstanceOfSatisfying(PaymentException.class, exception -> {
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.PAYMENT_GATEWAY_CONFIGURATION_ERROR);
                assertThat(exception.getProviderCode()).isEqualTo("INVALID_API_KEY");
            });
        server.verify();
    }

    @Test
    void convertsMalformedProviderErrorBodyToPaymentException() {
        server.expect(requestTo("https://api.tosspayments.com/v1/payments/payment-key"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                .body("<html>temporary gateway error</html>")
                .contentType(MediaType.TEXT_HTML));

        assertThatThrownBy(() -> adapter.findByPaymentKey("payment-key"))
            .isInstanceOfSatisfying(PaymentException.class, exception -> {
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE);
                assertThat(exception.getProviderCode()).isNull();
            });
        server.verify();
    }
}
