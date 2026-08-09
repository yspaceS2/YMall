package com.ymall.backend.order.returnrequest.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.order.returnrequest.dto.ReturnRequestCreateRequest;
import com.ymall.backend.order.returnrequest.dto.ReturnRequestResponse;
import com.ymall.backend.order.returnrequest.dto.ReturnRequestReviewRequest;
import com.ymall.backend.order.returnrequest.entity.ReturnRequestStatus;
import com.ymall.backend.payment.refund.dto.PaymentRefundItemRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;
import com.ymall.backend.payment.refund.service.PaymentRefundService;

@Service
@RequiredArgsConstructor
public class ReturnRequestService {

    private final ReturnRequestTransactionService transactionService;
    private final PaymentRefundService paymentRefundService;

    public ReturnRequestResponse create(
        Long memberId,
        Long orderId,
        ReturnRequestCreateRequest request
    ) {
        return transactionService.create(memberId, orderId, request);
    }

    public List<ReturnRequestResponse> getMemberRequests(Long memberId, Long orderId) {
        return transactionService.getMemberRequests(memberId, orderId);
    }

    public PageResponse<ReturnRequestResponse> getSellerRequests(
        Long memberId,
        int page,
        int size,
        ReturnRequestStatus status,
        String keyword
    ) {
        return transactionService.getSellerRequests(
            memberId,
            page,
            size,
            status,
            keyword
        );
    }

    public ReturnRequestResponse getSellerRequest(
        Long memberId,
        Long returnRequestId
    ) {
        return transactionService.getSellerRequest(memberId, returnRequestId);
    }

    /**
     * 반품 요청을 환불과 연결해 승인한다.
     *
     * <p>승인 상태를 먼저 확정하지 않고 환불 준비·결제사 취소·환불 완료가 끝난 뒤 반품을 승인한다.
     * 반품 요청 ID로 환불 멱등성 키를 고정하여 승인 재시도가 중복 환불로 이어지지 않게 한다.</p>
     */
    public ReturnRequestResponse approve(
        Long memberId,
        Long returnRequestId,
        ReturnRequestReviewRequest request
    ) {
        ReturnApprovalCommand command = transactionService.prepareApproval(
            memberId,
            returnRequestId
        );
        PaymentRefundResponse refund = paymentRefundService.refundSellerReturn(
            memberId,
            command.orderId(),
            new PaymentRefundRequest(
                "return-request-" + command.returnRequestId(),
                "반품 승인: " + command.reason(),
                List.of(new PaymentRefundItemRequest(
                    command.orderItemId(),
                    command.quantity()
                ))
            )
        );
        return transactionService.completeApproval(
            memberId,
            returnRequestId,
            refund.refundId(),
            request.response()
        );
    }

    public ReturnRequestResponse reject(
        Long memberId,
        Long returnRequestId,
        ReturnRequestReviewRequest request
    ) {
        return transactionService.reject(memberId, returnRequestId, request.response());
    }
}
