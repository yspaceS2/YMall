package com.ymall.backend.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.messaging.outbox.OrderOutboxService;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.dto.MockPaymentRequest;
import com.ymall.backend.payment.dto.PaymentConfirmRequest;
import com.ymall.backend.payment.dto.PaymentResponse;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.exception.PaymentException;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.mapper.PaymentMapper;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private OrderOutboxService orderOutboxService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PaymentGateway paymentGateway;

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

    @Test
    void confirmsPaymentAfterValidatingServerOrderAmount() {
        Order order = orderWithItem();
        PaymentConfirmRequest request = confirmationRequest(order, "payment-key-1", BigDecimal.valueOf(10000));
        PaymentResponse response = response(PaymentResult.SUCCESS, OrderStatus.PAID);
        PaymentGatewayResult gatewayResult = gatewayResult(request);

        given(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
            .willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(10L, "confirmation-1"))
            .willReturn(Optional.empty());
        given(paymentRepository.findByPaymentKey("payment-key-1"))
            .willReturn(Optional.empty());
        given(paymentGateway.confirm(any())).willReturn(gatewayResult);
        given(paymentRepository.saveAndFlush(any(Payment.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(paymentMapper.toPaymentResponse(any(Payment.class))).willReturn(response);

        PaymentResponse result = paymentService.confirmPayment(1L, 10L, request);

        assertThat(result).isSameAs(response);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        then(paymentGateway).should(times(1)).confirm(any());
    }

    @Test
    void translatesConcurrentPaymentKeyConstraintViolationToConflict() {
        Order order = orderWithItem();
        PaymentConfirmRequest request = confirmationRequest(
            order,
            "payment-key-1",
            BigDecimal.valueOf(10000)
        );

        given(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
            .willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(10L, "confirmation-1"))
            .willReturn(Optional.empty());
        given(paymentRepository.findByPaymentKey("payment-key-1"))
            .willReturn(Optional.empty());
        given(paymentGateway.confirm(any())).willReturn(gatewayResult(request));
        given(paymentRepository.saveAndFlush(any(Payment.class)))
            .willThrow(new DataIntegrityViolationException("duplicate payment key"));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, 10L, request))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_KEY_CONFLICT)
            );
    }

    @Test
    void rejectsClientAmountThatDiffersFromOrderAmount() {
        Order order = orderWithItem();
        PaymentConfirmRequest request = confirmationRequest(order, "payment-key-1", BigDecimal.valueOf(9000));

        given(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
            .willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(10L, "confirmation-1"))
            .willReturn(Optional.empty());
        given(paymentRepository.findByPaymentKey("payment-key-1"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, 10L, request))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
            );

        then(paymentGateway).shouldHaveNoInteractions();
    }

    @Test
    void returnsExistingPaymentForSamePaymentKey() {
        Order order = orderWithItem();
        order.completePayment();
        Payment payment = Payment.success(
            order,
            "confirmation-1",
            "payment-key-1",
            order.getPaymentOrderId(),
            BigDecimal.valueOf(10000),
            BigDecimal.valueOf(10000),
            "CARD",
            OffsetDateTime.now()
        );
        PaymentResponse response = response(PaymentResult.SUCCESS, OrderStatus.PAID);
        PaymentConfirmRequest request = confirmationRequest(order, "payment-key-1", BigDecimal.valueOf(10000));

        given(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
            .willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(10L, "confirmation-1"))
            .willReturn(Optional.empty());
        given(paymentRepository.findByPaymentKey("payment-key-1"))
            .willReturn(Optional.of(payment));
        given(paymentMapper.toPaymentResponse(payment)).willReturn(response);

        PaymentResponse result = paymentService.confirmPayment(1L, 10L, request);

        assertThat(result).isSameAs(response);
        then(paymentGateway).shouldHaveNoInteractions();
    }

    @Test
    void restoresInventoryWhenGatewayConfirmationFails() {
        Product product = product(10);
        ReflectionTestUtils.setField(product, "id", 100L);
        Order order = order();
        order.addItem(new OrderItem(
            product,
            product.getName(),
            BigDecimal.valueOf(10000),
            2
        ));
        product.decreaseStock(2);
        PaymentConfirmRequest request = confirmationRequest(order, "payment-key-1", BigDecimal.valueOf(20000));
        PaymentException gatewayException = new PaymentException(
            ErrorCode.PAYMENT_GATEWAY_ERROR,
            "REJECT_CARD_PAYMENT",
            "Card payment was rejected."
        );

        given(orderRepository.findByIdAndMemberIdForUpdate(10L, 1L))
            .willReturn(Optional.of(order));
        given(paymentRepository.findByOrderIdAndIdempotencyKey(10L, "confirmation-1"))
            .willReturn(Optional.empty());
        given(paymentRepository.findByPaymentKey("payment-key-1"))
            .willReturn(Optional.empty());
        given(paymentGateway.confirm(any())).willThrow(gatewayException);
        given(productRepository.findAllByIdForUpdate(List.of(100L)))
            .willReturn(List.of(product));
        given(paymentRepository.saveAndFlush(any(Payment.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, 10L, request))
            .isSameAs(gatewayException);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(order.isInventoryReserved()).isFalse();
        assertThat(product.getStock()).isEqualTo(10);
        then(paymentRepository).should().saveAndFlush(any(Payment.class));
    }

    private Order order() {
        Order order = new Order(
            new Member("user@example.com", "password", "홍길동", MemberRole.ROLE_USER),
            "order-1"
        );
        ReflectionTestUtils.setField(order, "id", 10L);
        return order;
    }

    private Order orderWithItem() {
        Order order = order();
        Product product = product(10);
        order.addItem(new OrderItem(
            product,
            product.getName(),
            BigDecimal.valueOf(10000),
            1
        ));
        return order;
    }

    private Product product(int stock) {
        return new Product(
            new Category("Category", "category"),
            "Payment product",
            "description",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(4.5),
            stock,
            "thumbnail",
            ProductStatus.APPROVED
        );
    }

    private PaymentConfirmRequest confirmationRequest(
        Order order,
        String paymentKey,
        BigDecimal amount
    ) {
        return new PaymentConfirmRequest(
            paymentKey,
            order.getPaymentOrderId(),
            amount,
            "confirmation-1"
        );
    }

    private PaymentGatewayResult gatewayResult(PaymentConfirmRequest request) {
        return new PaymentGatewayResult(
            request.paymentKey(),
            request.paymentOrderId(),
            PaymentGatewayStatus.DONE,
            request.amount(),
            BigDecimal.ZERO,
            "CARD",
            OffsetDateTime.now()
        );
    }

    private PaymentResponse response(PaymentResult result, OrderStatus orderStatus) {
        return new PaymentResponse(
            1L,
            10L,
            null,
            null,
            null,
            null,
            null,
            null,
            result,
            orderStatus,
            null,
            result == PaymentResult.FAILURE ? "실패" : null,
            LocalDateTime.now()
        );
    }
}
