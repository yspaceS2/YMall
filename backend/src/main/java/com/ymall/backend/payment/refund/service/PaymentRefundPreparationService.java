package com.ymall.backend.payment.refund.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.dashboard.service.DashboardRealtimePublisher;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.refund.dto.PaymentRefundRequest;
import com.ymall.backend.payment.refund.entity.PaymentRefund;
import com.ymall.backend.payment.refund.entity.PaymentRefundItem;
import com.ymall.backend.payment.refund.entity.PaymentRefundStatus;
import com.ymall.backend.payment.refund.entity.PaymentRefundType;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerProfileRepository;

@Service
@RequiredArgsConstructor
class PaymentRefundPreparationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final DashboardRealtimePublisher dashboardRealtimePublisher;
    private final PaymentRefundCalculator calculator;
    private final PaymentRefundResponseMapper responseMapper;

    @Transactional
    PaymentRefundPreparation prepareUser(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        Order order = orderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return prepareRefund(order, memberId, MemberRole.ROLE_USER, request, item -> true, false);
    }

    @Transactional
    PaymentRefundPreparation prepareSeller(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        SellerProfile profile = sellerProfileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
        Order order = orderRepository.findSellerOrderByIdForUpdate(orderId, profile.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_ORDER_NOT_FOUND));
        return prepareRefund(
            order,
            memberId,
            MemberRole.ROLE_SELLER,
            request,
            item -> calculator.isOwnedBySeller(item, profile.getId()),
            false
        );
    }

    @Transactional
    PaymentRefundPreparation prepareSellerReturn(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        SellerProfile profile = sellerProfileRepository.findByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_PROFILE_NOT_FOUND));
        Order order = orderRepository.findSellerOrderByIdForUpdate(orderId, profile.getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_ORDER_NOT_FOUND));
        return prepareRefund(
            order,
            memberId,
            MemberRole.ROLE_SELLER,
            request,
            item -> calculator.isOwnedBySeller(item, profile.getId()),
            true
        );
    }

    @Transactional
    PaymentRefundPreparation prepareAdmin(
        Long memberId,
        Long orderId,
        PaymentRefundRequest request
    ) {
        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return prepareRefund(order, memberId, MemberRole.ROLE_ADMIN, request, item -> true, false);
    }

    private PaymentRefundPreparation prepareRefund(
        Order order,
        Long memberId,
        MemberRole role,
        PaymentRefundRequest request,
        Predicate<OrderItem> itemAccess,
        boolean deliveredReturn
    ) {
        Long orderId = order.getId();
        PaymentRefund existing = refundRepository.findByOrderIdAndIdempotencyKey(
            orderId,
            request.idempotencyKey()
        ).orElse(null);
        if (existing != null) {
            if (role == MemberRole.ROLE_SELLER
                && existing.getItems().stream()
                    .anyMatch(item -> !itemAccess.test(item.getOrderItem()))) {
                throw new BusinessException(ErrorCode.SELLER_ORDER_NOT_FOUND);
            }
            if (existing.getStatus() == PaymentRefundStatus.UNKNOWN) {
                throw new BusinessException(ErrorCode.PAYMENT_REFUND_RECONCILIATION_REQUIRED);
            }
            return PaymentRefundPreparation.existing(
                responseMapper.toResponse(
                    existing,
                    item -> itemAccess.test(item.getOrderItem())
                )
            );
        }

        calculator.validateOrderStatus(order, deliveredReturn);
        Payment payment = paymentRepository
            .findFirstByOrderIdAndResultOrderByProcessedAtDesc(orderId, PaymentResult.SUCCESS)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED));
        if (payment.getPaymentKey() == null) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED);
        }
        List<PaymentRefund> priorRefunds = refundRepository
            .findAllByOrderIdOrderByCreatedAtDesc(orderId);
        if (priorRefunds.stream().anyMatch(refund ->
            refund.getStatus() == PaymentRefundStatus.PENDING
                || refund.getStatus() == PaymentRefundStatus.UNKNOWN
        )) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_RECONCILIATION_REQUIRED);
        }

        Map<Long, Integer> pendingQuantities = calculator.pendingQuantities(priorRefunds);
        List<PaymentRefundCalculator.RefundQuantity> quantities = calculator.resolveQuantities(
            order,
            request.items(),
            pendingQuantities,
            itemAccess,
            deliveredReturn
        );
        BigDecimal remainingAmount = calculator.remainingAmount(order, priorRefunds);
        BigDecimal requestedAmount = quantities.stream()
            .map(quantity -> calculator.calculateRefundAmount(quantity, pendingQuantities))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (requestedAmount.signum() <= 0
            || requestedAmount.compareTo(remainingAmount) > 0) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_AMOUNT_EXCEEDED);
        }

        PaymentRefund refund = new PaymentRefund(
            payment,
            order,
            request.idempotencyKey(),
            requestedAmount.compareTo(remainingAmount) == 0
                ? PaymentRefundType.FULL
                : PaymentRefundType.PARTIAL,
            request.reason().trim(),
            memberId,
            role
        );
        quantities.forEach(quantity -> refund.addItem(new PaymentRefundItem(
            quantity.item(),
            quantity.quantity(),
            calculator.calculateRefundAmount(quantity, pendingQuantities)
        )));
        refundRepository.save(refund);
        dashboardRealtimePublisher.invalidateOrder(orderId);
        return new PaymentRefundPreparation(
            refund.getId(),
            payment.getPaymentKey(),
            refund.getAmount(),
            refund.getReason(),
            refund.getIdempotencyKey(),
            null
        );
    }
}
