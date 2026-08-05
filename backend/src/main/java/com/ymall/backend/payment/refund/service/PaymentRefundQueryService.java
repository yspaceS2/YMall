package com.ymall.backend.payment.refund.service;

import java.util.List;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;
import com.ymall.backend.payment.refund.entity.PaymentRefundItem;
import com.ymall.backend.payment.refund.entity.PaymentRefundStatus;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerProfileRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PaymentRefundQueryService {

    private final OrderRepository orderRepository;
    private final PaymentRefundRepository refundRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final PaymentRefundCalculator calculator;
    private final PaymentRefundResponseMapper responseMapper;

    List<PaymentRefundReconciliationCandidate> getUnknownRefunds(Long orderId) {
        return refundRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId)
            .stream()
            .filter(refund -> refund.getStatus() == PaymentRefundStatus.UNKNOWN)
            .map(refund -> new PaymentRefundReconciliationCandidate(
                refund.getId(),
                refund.getPayment().getPaymentKey()
            ))
            .toList();
    }

    List<PaymentRefundResponse> getRefunds(Long memberId, Long orderId) {
        orderRepository.findByIdAndMemberId(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return refundRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId)
            .stream()
            .map(responseMapper::toResponse)
            .toList();
    }

    List<PaymentRefundResponse> getSellerRefunds(Long memberId, Long orderId) {
        SellerProfile profile = sellerProfileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
        orderRepository.findSellerOrderById(orderId, profile.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_ORDER_NOT_FOUND));
        Predicate<PaymentRefundItem> itemAccess = refundItem ->
            calculator.isOwnedBySeller(refundItem.getOrderItem(), profile.getId());
        return refundRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId)
            .stream()
            .filter(refund -> refund.getItems().stream().anyMatch(itemAccess))
            .map(refund -> responseMapper.toResponse(refund, itemAccess))
            .toList();
    }

    List<PaymentRefundResponse> getAdminRefunds(Long orderId) {
        orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return refundRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId)
            .stream()
            .map(responseMapper::toResponse)
            .toList();
    }
}
