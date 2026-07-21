package com.ymall.backend.payment.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.global.messaging.outbox.OrderOutboxService;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.dto.MockPaymentRequest;
import com.ymall.backend.payment.dto.PaymentResponse;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.mapper.PaymentMapper;
import com.ymall.backend.payment.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final String MOCK_FAILURE_MESSAGE = "모의 결제에 실패했습니다. 다시 시도해 주세요.";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderOutboxService orderOutboxService;

    @Transactional
    public PaymentResponse processPayment(
        Long memberId,
        Long orderId,
        MockPaymentRequest request
    ) {
        Order order = orderRepository.findByIdAndMemberIdForUpdate(orderId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return paymentRepository.findByOrderIdAndIdempotencyKey(orderId, request.idempotencyKey())
            .map(paymentMapper::toPaymentResponse)
            .orElseGet(() -> processNewPayment(order, request));
    }

    private PaymentResponse processNewPayment(Order order, MockPaymentRequest request) {
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
            && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_ALLOWED);
        }

        String failureMessage = null;
        if (request.result() == PaymentResult.SUCCESS) {
            order.completePayment();
        } else {
            order.failPayment();
            failureMessage = MOCK_FAILURE_MESSAGE;
        }

        Payment payment = paymentRepository.save(new Payment(
            order,
            request.idempotencyKey(),
            request.result(),
            failureMessage
        ));
        OrderEventType eventType = request.result() == PaymentResult.SUCCESS
            ? OrderEventType.PAYMENT_COMPLETED
            : OrderEventType.PAYMENT_FAILED;
        orderOutboxService.save(
            eventType,
            order.getId(),
            order.getMember().getId(),
            Map.of(
                "status", order.getStatus().name(),
                "paymentResult", request.result().name()
            )
        );
        return paymentMapper.toPaymentResponse(payment);
    }
}
