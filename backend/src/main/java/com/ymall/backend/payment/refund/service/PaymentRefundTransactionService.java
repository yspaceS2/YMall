package com.ymall.backend.payment.refund.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.refund.dto.PaymentRefundRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;

@Service
@RequiredArgsConstructor
public class PaymentRefundTransactionService {

    private final PaymentRefundPreparationService preparationService;
    private final PaymentRefundCompletionService completionService;
    private final PaymentRefundQueryService queryService;

    public PaymentRefundPreparation prepareUser(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        return preparationService.prepareUser(memberId, orderId, request);
    }

    public PaymentRefundPreparation prepareSeller(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        return preparationService.prepareSeller(memberId, orderId, request);
    }

    public PaymentRefundPreparation prepareSellerReturn(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        return preparationService.prepareSellerReturn(memberId, orderId, request);
    }

    public PaymentRefundPreparation prepareAdmin(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        return preparationService.prepareAdmin(memberId, orderId, request);
    }

    public PaymentRefundResponse complete(
        Long refundId,
        PaymentGatewayResult gatewayResult
    ) {
        return completionService.complete(refundId, gatewayResult);
    }

    public List<PaymentRefundReconciliationCandidate> getUnknownRefunds(Long orderId) {
        return queryService.getUnknownRefunds(orderId);
    }

    public boolean reconcile(Long refundId, PaymentGatewayResult gatewayResult) {
        return completionService.reconcile(refundId, gatewayResult);
    }

    public void recordFailure(
        Long refundId,
        String failureCode,
        String failureMessage,
        boolean outcomeUnknown
    ) {
        completionService.recordFailure(
            refundId,
            failureCode,
            failureMessage,
            outcomeUnknown
        );
    }

    public List<PaymentRefundResponse> getRefunds(Long memberId, Long orderId) {
        return queryService.getRefunds(memberId, orderId);
    }

    public List<PaymentRefundResponse> getSellerRefunds(Long memberId, Long orderId) {
        return queryService.getSellerRefunds(memberId, orderId);
    }

    public List<PaymentRefundResponse> getAdminRefunds(Long orderId) {
        return queryService.getAdminRefunds(orderId);
    }
}
