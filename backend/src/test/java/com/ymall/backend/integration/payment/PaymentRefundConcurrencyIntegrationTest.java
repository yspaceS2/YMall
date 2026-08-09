package com.ymall.backend.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.refund.dto.PaymentRefundItemRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundRequest;
import com.ymall.backend.payment.refund.dto.PaymentRefundResponse;
import com.ymall.backend.payment.refund.entity.PaymentRefundStatus;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.payment.refund.service.PaymentRefundService;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.testsupport.PostgresIntegrationTestSupport;

@SpringBootTest
@ActiveProfiles("test")
class PaymentRefundConcurrencyIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private PaymentRefundService refundService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentRefundRepository refundRepository;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @Test
    void blocksSecondRefundWhileFirstProviderRequestIsPending() throws Exception {
        Member member = memberRepository.save(new Member(
            "refund-concurrency@example.com",
            "password",
            "Refund Concurrency User",
            MemberRole.ROLE_USER
        ));
        Category category = categoryRepository.save(new Category(
            "Refund concurrency",
            "refund-concurrency"
        ));
        Product product = productRepository.save(new Product(
            category,
            "Refund concurrency product",
            "description",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(4.5),
            10,
            "thumbnail",
            ProductStatus.APPROVED
        ));
        product.decreaseStock(2);
        productRepository.saveAndFlush(product);
        Order order = new Order(member, "refund-concurrency-order");
        order.addItem(new OrderItem(product, product.getName(), product.getPrice(), 2));
        order.completePayment();
        order = orderRepository.saveAndFlush(order);
        paymentRepository.saveAndFlush(Payment.success(
            order,
            "refund-concurrency-payment",
            "refund-concurrency-payment-key",
            order.getPaymentOrderId(),
            order.getTotalAmount(),
            order.getTotalAmount(),
            "CARD",
            OffsetDateTime.now()
        ));
        Long orderId = order.getId();
        Long orderItemId = order.getItems().get(0).getId();
        String paymentOrderId = order.getPaymentOrderId();
        CountDownLatch gatewayEntered = new CountDownLatch(1);
        CountDownLatch gatewayRelease = new CountDownLatch(1);
        given(paymentGateway.cancel(any())).willAnswer(invocation -> {
            gatewayEntered.countDown();
            gatewayRelease.await(10, TimeUnit.SECONDS);
            return new PaymentGatewayResult(
                "refund-concurrency-payment-key",
                paymentOrderId,
                PaymentGatewayStatus.PARTIAL_CANCELED,
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(10000),
                "CARD",
                OffsetDateTime.now()
            );
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<PaymentRefundResponse> first = executor.submit(() ->
                refundService.refundUser(
                    member.getId(),
                    orderId,
                    request("first-refund", orderItemId)
                )
            );
            assertThat(gatewayEntered.await(10, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> refundService.refundUser(
                member.getId(),
                orderId,
                request("second-refund", orderItemId)
            ))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                    assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_REFUND_RECONCILIATION_REQUIRED)
                );

            gatewayRelease.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS).status())
                .isEqualTo(PaymentRefundStatus.SUCCEEDED);
        } finally {
            gatewayRelease.countDown();
            executor.shutdownNow();
        }

        assertThat(refundRepository.findAll()).hasSize(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
            .isEqualTo(9);
    }

    private PaymentRefundRequest request(String idempotencyKey, Long orderItemId) {
        return new PaymentRefundRequest(
            idempotencyKey,
            "Refund concurrency test",
            List.of(new PaymentRefundItemRequest(orderItemId, 1))
        );
    }
}
