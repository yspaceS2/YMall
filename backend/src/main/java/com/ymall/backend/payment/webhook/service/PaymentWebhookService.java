package com.ymall.backend.payment.webhook.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.webhook.dto.TossPaymentWebhookRequest;
import com.ymall.backend.payment.webhook.repository.PaymentWebhookEventRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookService {

    private static final String PAYMENT_STATUS_CHANGED = "PAYMENT_STATUS_CHANGED";

    private final PaymentWebhookEventRepository webhookEventRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentWebhookTransactionService transactionService;

    public void handle(String transmissionId, TossPaymentWebhookRequest request) {
        if (webhookEventRepository.existsByTransmissionId(transmissionId)) {
            return;
        }
        if (!PAYMENT_STATUS_CHANGED.equals(request.eventType())) {
            throw new BusinessException(ErrorCode.PAYMENT_WEBHOOK_UNSUPPORTED_EVENT);
        }

        try {
            PaymentGatewayStatus requestedStatus = parseStatus(request.data().status());
            PaymentGatewayResult verifiedPayment = paymentGateway.findByPaymentKey(
                request.data().paymentKey()
            );
            transactionService.process(
                transmissionId,
                request,
                requestedStatus,
                verifiedPayment
            );
        } catch (RuntimeException exception) {
            log.warn(
                "Payment webhook processing failed. transmissionId={}, eventType={}",
                safeLogValue(transmissionId),
                safeLogValue(request.eventType()),
                exception
            );
            throw exception;
        }
    }

    private PaymentGatewayStatus parseStatus(String status) {
        try {
            PaymentGatewayStatus parsedStatus = PaymentGatewayStatus.valueOf(status);
            if (parsedStatus == PaymentGatewayStatus.UNKNOWN) {
                throw new IllegalArgumentException();
            }
            return parsedStatus;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.PAYMENT_WEBHOOK_INVALID, exception);
        }
    }

    private String safeLogValue(String value) {
        String sanitized = value.replaceAll("[^A-Za-z0-9._:-]", "_");
        return sanitized.substring(0, Math.min(sanitized.length(), 64));
    }
}
