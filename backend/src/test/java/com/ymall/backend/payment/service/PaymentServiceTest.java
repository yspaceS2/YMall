package com.ymall.backend.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.dto.MockPaymentRequest;
import com.ymall.backend.payment.dto.PaymentResponse;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.mapper.PaymentMapper;
import com.ymall.backend.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void completesOrderWhenMockPaymentSucceeds() {
        Order order = order();
        MockPaymentRequest request = new MockPaymentRequest("payment-1", PaymentResult.SUCCESS);
        PaymentResponse response = response(PaymentResult.SUCCESS, OrderStatus.PAID);

        given(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
            .willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(10L, "payment-1"))
            .willReturn(Optional.empty());
        given(paymentRepository.save(any(Payment.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(paymentMapper.toPaymentResponse(any(Payment.class))).willReturn(response);

        PaymentResponse result = paymentService.processPayment(1L, 10L, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.result()).isEqualTo(PaymentResult.SUCCESS);
        then(paymentRepository).should().save(any(Payment.class));
    }

    @Test
    void returnsExistingPaymentForDuplicateIdempotencyKey() {
        Order order = order();
        Payment payment = new Payment(order, "payment-1", PaymentResult.FAILURE, "실패");
        PaymentResponse response = response(PaymentResult.FAILURE, OrderStatus.PAYMENT_FAILED);

        given(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
            .willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(10L, "payment-1"))
            .willReturn(Optional.of(payment));
        given(paymentMapper.toPaymentResponse(payment)).willReturn(response);

        PaymentResponse result = paymentService.processPayment(
            1L,
            10L,
            new MockPaymentRequest("payment-1", PaymentResult.FAILURE)
        );

        assertThat(result).isSameAs(response);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        then(paymentRepository).shouldHaveNoMoreInteractions();
    }

    private Order order() {
        return new Order(
            new Member("user@example.com", "password", "홍길동", MemberRole.ROLE_USER),
            "order-1"
        );
    }

    private PaymentResponse response(PaymentResult result, OrderStatus orderStatus) {
        return new PaymentResponse(
            1L,
            10L,
            result,
            orderStatus,
            result == PaymentResult.FAILURE ? "실패" : null,
            LocalDateTime.now()
        );
    }
}
