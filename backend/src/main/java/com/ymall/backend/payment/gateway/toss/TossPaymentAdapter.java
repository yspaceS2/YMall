package com.ymall.backend.payment.gateway.toss;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.payment.exception.PaymentException;
import com.ymall.backend.payment.gateway.PaymentCancelCommand;
import com.ymall.backend.payment.gateway.PaymentConfirmCommand;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;

@Component
public class TossPaymentAdapter implements PaymentGateway {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final TossPaymentProperties properties;
    private final TossPaymentMapper paymentMapper;
    private final TossPaymentExceptionMapper exceptionMapper;

    public TossPaymentAdapter(
        RestClient tossPaymentRestClient,
        ObjectMapper objectMapper,
        TossPaymentProperties properties,
        TossPaymentMapper paymentMapper,
        TossPaymentExceptionMapper exceptionMapper
    ) {
        this.restClient = tossPaymentRestClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.paymentMapper = paymentMapper;
        this.exceptionMapper = exceptionMapper;
    }

    @Override
    public PaymentGatewayResult confirm(PaymentConfirmCommand command) {
        validateCredentials();
        TossConfirmRequest request = new TossConfirmRequest(
            command.paymentKey(),
            command.orderId(),
            toTossAmount(command.amount())
        );

        return execute(() -> restClient.post()
            .uri("/v1/payments/confirm")
            .header("Idempotency-Key", command.idempotencyKey())
            .body(request)
            .retrieve()
            .onStatus(status -> status.isError(), this::handleError)
            .body(TossPaymentResponse.class));
    }

    @Override
    public PaymentGatewayResult findByPaymentKey(String paymentKey) {
        validateCredentials();
        return execute(() -> restClient.get()
            .uri("/v1/payments/{paymentKey}", paymentKey)
            .retrieve()
            .onStatus(status -> status.isError(), this::handleError)
            .body(TossPaymentResponse.class));
    }

    @Override
    public PaymentGatewayResult cancel(PaymentCancelCommand command) {
        validateCredentials();
        TossCancelRequest request = new TossCancelRequest(
            command.reason(),
            command.cancelAmount() == null ? null : toTossAmount(command.cancelAmount())
        );

        return execute(() -> restClient.post()
            .uri("/v1/payments/{paymentKey}/cancel", command.paymentKey())
            .header("Idempotency-Key", command.idempotencyKey())
            .body(request)
            .retrieve()
            .onStatus(status -> status.isError(), this::handleError)
            .body(TossPaymentResponse.class));
    }

    private PaymentGatewayResult execute(Supplier<TossPaymentResponse> request) {
        try {
            TossPaymentResponse response = request.get();
            if (response == null) {
                throw new PaymentException(
                    ErrorCode.PAYMENT_GATEWAY_ERROR,
                    "EMPTY_RESPONSE",
                    "Toss Payments returned an empty response."
                );
            }
            return paymentMapper.toResult(response);
        } catch (ResourceAccessException exception) {
            if (hasCause(exception, SocketTimeoutException.class)
                || hasCause(exception, HttpTimeoutException.class)) {
                throw new PaymentException(ErrorCode.PAYMENT_GATEWAY_TIMEOUT, exception);
            }
            throw new PaymentException(ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE, exception);
        }
    }

    private void handleError(
        org.springframework.http.HttpRequest request,
        ClientHttpResponse response
    ) throws IOException {
        TossPaymentErrorResponse errorResponse = objectMapper.readValue(
            response.getBody(),
            TossPaymentErrorResponse.class
        );
        throw exceptionMapper.map(response.getStatusCode(), errorResponse);
    }

    private long toTossAmount(BigDecimal amount) {
        try {
            return amount.longValueExact();
        } catch (ArithmeticException exception) {
            throw new PaymentException(
                ErrorCode.PAYMENT_GATEWAY_ERROR,
                "INVALID_AMOUNT",
                "Payment amount must be an integer."
            );
        }
    }

    private void validateCredentials() {
        if (!properties.hasCredentials()) {
            throw new PaymentException(
                ErrorCode.PAYMENT_GATEWAY_CONFIGURATION_ERROR,
                "MISSING_API_KEY",
                "Toss Payments client key and secret key are required."
            );
        }
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record TossConfirmRequest(
        String paymentKey,
        String orderId,
        long amount
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record TossCancelRequest(
        String cancelReason,
        Long cancelAmount
    ) {
    }
}
