package com.ymall.backend.payment.refund.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.messaging.outbox.OrderOutboxService;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.refund.dto.PaymentRefundItemRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundRequest;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.service.ProductCacheInvalidator;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerProfileRepository;

class PaymentRefundTransactionServiceTest {

    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private PaymentRefundRepository refundRepository;
    private SellerProfileRepository sellerProfileRepository;
    private PaymentRefundTransactionService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        refundRepository = mock(PaymentRefundRepository.class);
        sellerProfileRepository = mock(SellerProfileRepository.class);
        service = new PaymentRefundTransactionService(
            orderRepository,
            paymentRepository,
            refundRepository,
            mock(ProductRepository.class),
            mock(ProductCacheInvalidator.class),
            sellerProfileRepository,
            mock(OrderOutboxService.class)
        );
    }

    @Test
    void permitsDeliveredItemOnlyThroughSellerReturnApprovalPath() {
        Long memberId = 1L;
        Long orderId = 10L;
        Long orderItemId = 20L;
        SellerProfile sellerProfile = mock(SellerProfile.class);
        Product product = mock(Product.class);
        OrderItem orderItem = mock(OrderItem.class);
        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);
        PaymentRefundRequest request = new PaymentRefundRequest(
            "return-request-1",
            "반품 승인",
            List.of(new PaymentRefundItemRequest(orderItemId, 1))
        );

        given(sellerProfile.getId()).willReturn(2L);
        given(sellerProfileRepository.findByMemberId(memberId))
            .willReturn(Optional.of(sellerProfile));
        given(orderRepository.findSellerOrderByIdForUpdate(orderId, 2L))
            .willReturn(Optional.of(order));
        given(order.getId()).willReturn(orderId);
        given(order.getStatus()).willReturn(OrderStatus.DELIVERED);
        given(order.getItems()).willReturn(List.of(orderItem));
        given(order.getTotalAmount()).willReturn(BigDecimal.valueOf(13000));
        given(orderItem.getId()).willReturn(orderItemId);
        given(orderItem.getProduct()).willReturn(product);
        given(product.getSellerProfile()).willReturn(sellerProfile);
        given(orderItem.getEffectiveFulfillmentStatus())
            .willReturn(OrderItemFulfillmentStatus.DELIVERED);
        given(orderItem.getRefundableQuantity()).willReturn(1);
        given(orderItem.getUnitPrice()).willReturn(BigDecimal.valueOf(10000));
        given(orderItem.getShippingFee()).willReturn(BigDecimal.valueOf(3000));
        given(refundRepository.findByOrderIdAndIdempotencyKey(
            orderId,
            request.idempotencyKey()
        )).willReturn(Optional.empty());
        given(paymentRepository.findFirstByOrderIdAndResultOrderByProcessedAtDesc(
            orderId,
            PaymentResult.SUCCESS
        )).willReturn(Optional.of(payment));
        given(payment.getPaymentKey()).willReturn("payment-key");
        given(refundRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId))
            .willReturn(List.of());

        PaymentRefundPreparation preparation = service.prepareSellerReturn(
            memberId,
            orderId,
            request
        );

        assertThat(preparation.amount()).isEqualByComparingTo("13000");
        assertThatThrownBy(() -> service.prepareSeller(memberId, orderId, request))
            .isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED)
            );
    }
}
