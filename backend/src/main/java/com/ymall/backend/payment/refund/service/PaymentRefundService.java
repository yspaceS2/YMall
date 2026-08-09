package com.ymall.backend.payment.refund.service;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.payment.exception.PaymentException;
import com.ymall.backend.payment.gateway.PaymentCancelCommand;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.refund.dto.PaymentRefundRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;

/**
 * 환불 상태 변경 트랜잭션과 외부 결제사 호출을 분리해 조정한다.
 *
 * <p>DB 잠금을 유지한 채 네트워크를 호출하지 않도록 먼저 환불을 준비하고, 결제사 호출 후 별도
 * 트랜잭션에서 완료한다. Timeout처럼 결과를 확정할 수 없는 실패는 UNKNOWN으로 기록하고 결제사의
 * 현재 상태를 조회해 조정하기 전까지 같은 주문의 새 환불을 허용하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class PaymentRefundService {

    private final PaymentRefundTransactionService transactionService;
    private final PaymentGateway paymentGateway;

    public PaymentRefundResponse refundUser(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        return executeWithReconciliation(
            orderId,
            () -> transactionService.prepareUser(memberId, orderId, request)
        );
    }

    public PaymentRefundResponse refundSeller(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        return executeWithReconciliation(
            orderId,
            () -> transactionService.prepareSeller(memberId, orderId, request)
        );
    }

    public PaymentRefundResponse refundSellerReturn(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        return executeWithReconciliation(
            orderId,
            () -> transactionService.prepareSellerReturn(memberId, orderId, request)
        );
    }

    public PaymentRefundResponse refundAdmin(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        return executeWithReconciliation(
            orderId,
            () -> transactionService.prepareAdmin(memberId, orderId, request)
        );
    }

    private PaymentRefundResponse executeWithReconciliation(
        Long orderId,
        Supplier<PaymentRefundPreparation> preparationSupplier
    ) {
        try {
            return execute(preparationSupplier.get());
        } catch (BusinessException exception) {
            if (exception.getErrorCode()
                != ErrorCode.PAYMENT_REFUND_RECONCILIATION_REQUIRED) {
                throw exception;
            }
            reconcileUnknownRefunds(orderId);
            return execute(preparationSupplier.get());
        }
    }

    private void reconcileUnknownRefunds(Long orderId) {
        List<PaymentRefundReconciliationCandidate> candidates =
            transactionService.getUnknownRefunds(orderId);
        if (candidates.isEmpty()) {
            throw new BusinessException(
                ErrorCode.PAYMENT_REFUND_RECONCILIATION_REQUIRED
            );
        }

        for (PaymentRefundReconciliationCandidate candidate : candidates) {
            PaymentGatewayResult result = paymentGateway.findByPaymentKey(
                candidate.paymentKey()
            );
            if (!transactionService.reconcile(candidate.refundId(), result)) {
                throw new BusinessException(
                    ErrorCode.PAYMENT_REFUND_RECONCILIATION_REQUIRED
                );
            }
        }
    }

    private PaymentRefundResponse execute(PaymentRefundPreparation preparation) {
        if (!preparation.requiresGatewayCall()) {
            return preparation.existingResponse();
        }

        try {
            PaymentGatewayResult result = paymentGateway.cancel(new PaymentCancelCommand(
                preparation.paymentKey(),
                preparation.reason(),
                preparation.amount(),
                preparation.idempotencyKey()
            ));
            return transactionService.complete(preparation.refundId(), result);
        } catch (PaymentException exception) {
            boolean outcomeUnknown = exception.getErrorCode() == ErrorCode.PAYMENT_GATEWAY_TIMEOUT
                || exception.getErrorCode() == ErrorCode.PAYMENT_GATEWAY_UNAVAILABLE;
            transactionService.recordFailure(
                preparation.refundId(),
                exception.getProviderCode(),
                exception.getProviderMessage(),
                outcomeUnknown
            );
            throw exception;
        } catch (BusinessException exception) {
            transactionService.recordFailure(
                preparation.refundId(),
                exception.getErrorCode().name(),
                exception.getMessage(),
                true
            );
            throw exception;
        } catch (RuntimeException exception) {
            transactionService.recordFailure(
                preparation.refundId(),
                "INTERNAL_ERROR",
                "Refund provider result requires reconciliation before retry.",
                true
            );
            throw exception;
        }
    }

    public List<PaymentRefundResponse> getRefunds(Long memberId, Long orderId) {
        return transactionService.getRefunds(memberId, orderId);
    }

    public List<PaymentRefundResponse> getSellerRefunds(Long memberId, Long orderId) {
        return transactionService.getSellerRefunds(memberId, orderId);
    }

    public List<PaymentRefundResponse> getAdminRefunds(Long orderId) {
        return transactionService.getAdminRefunds(orderId);
    }
}
