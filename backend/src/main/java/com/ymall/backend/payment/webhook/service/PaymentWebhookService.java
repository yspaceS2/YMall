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

    /**
     * 신뢰할 수 없는 웹훅을 Toss에서 결제 정보를 다시 조회한 뒤 처리한다.
     *
     * <p>웹훅 Payload는 결제를 식별하는 데만 사용하며 상태의 최종 근거로 신뢰하지 않는다.
     * 중복 전송 ID는 먼저 빠르게 제외하고, {@link PaymentWebhookTransactionService}가 주문 잠금을
     * 획득한 뒤 트랜잭션 안에서 다시 확인한다.</p>
     */
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
                "Payment webhook processing failed. transmissionId={}, eventType={}, errorType={}",
                safeLogValue(transmissionId),
                safeLogValue(request.eventType()),
                exception.getClass().getSimpleName()
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
