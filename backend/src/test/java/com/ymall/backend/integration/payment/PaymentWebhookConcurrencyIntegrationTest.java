package com.ymall.backend.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.global.messaging.outbox.OrderOutboxEventRepository;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.dto.OrderCreateRequest;
import com.ymall.backend.order.dto.OrderResponse;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.order.service.OrderService;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.payment.webhook.dto.TossPaymentWebhookRequest;
import com.ymall.backend.payment.webhook.repository.PaymentWebhookEventRepository;
import com.ymall.backend.payment.webhook.service.PaymentWebhookService;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.testsupport.PostgresIntegrationTestSupport;

@SpringBootTest
@ActiveProfiles("test")
class PaymentWebhookConcurrencyIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private PaymentWebhookService paymentWebhookService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberAddressRepository memberAddressRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentWebhookEventRepository webhookEventRepository;

    @Autowired
    private OrderOutboxEventRepository outboxEventRepository;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @Test
    void processesConcurrentWebhookWithSameTransmissionOnlyOnce() throws Exception {
        Member member = memberRepository.save(new Member(
            "webhook-concurrent@example.com",
            "password",
            "Concurrent Webhook User",
            MemberRole.ROLE_USER
        ));
        MemberAddress address = memberAddressRepository.save(new MemberAddress(
            member,
            "Home",
            "Recipient",
            "01012345678",
            "00000",
            "123 Test-ro",
            "101",
            true
        ));
        Category category = categoryRepository.save(new Category(
            "Concurrent webhook",
            "concurrent-webhook"
        ));
        Product product = productRepository.save(new Product(
            category,
            "Concurrent webhook product",
            "description",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(4.5),
            10,
            "thumbnail",
            ProductStatus.APPROVED
        ));
        cartItemRepository.save(new CartItem(member, product, 2));

        OrderResponse order = orderService.createOrder(
            member.getId(),
            new OrderCreateRequest("concurrent-webhook-order", address.getId())
        );
        TossPaymentWebhookRequest request = webhookRequest(order);
        CountDownLatch gatewayReady = new CountDownLatch(2);
        CountDownLatch gatewayRelease = new CountDownLatch(1);
        AtomicInteger gatewayCalls = new AtomicInteger();
        given(paymentGateway.findByPaymentKey(anyString())).willAnswer(invocation -> {
            gatewayCalls.incrementAndGet();
            gatewayReady.countDown();
            gatewayRelease.await(10, TimeUnit.SECONDS);
            return new PaymentGatewayResult(
                request.data().paymentKey(),
                request.data().orderId(),
                PaymentGatewayStatus.DONE,
                request.data().totalAmount(),
                BigDecimal.ZERO,
                "CARD",
                OffsetDateTime.now()
            );
        });

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Void>> responses = List.of(
                submitWebhook(executor, ready, start, request),
                submitWebhook(executor, ready, start, request)
            );

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(gatewayReady.await(10, TimeUnit.SECONDS)).isTrue();
            gatewayRelease.countDown();

            assertThat(responses.get(0).get(10, TimeUnit.SECONDS)).isNull();
            assertThat(responses.get(1).get(10, TimeUnit.SECONDS)).isNull();
        } finally {
            gatewayRelease.countDown();
            executor.shutdownNow();
        }

        assertThat(gatewayCalls).hasValue(2);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(webhookEventRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.count()).isEqualTo(2);
        assertThat(orderRepository.findById(order.orderId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PAID);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
            .isEqualTo(8);
    }

    private TossPaymentWebhookRequest webhookRequest(OrderResponse order) {
        return new TossPaymentWebhookRequest(
            "PAYMENT_STATUS_CHANGED",
            LocalDateTime.of(2026, 7, 24, 12, 0),
            new TossPaymentWebhookRequest.PaymentData(
                "concurrent-webhook-payment-key",
                order.paymentOrderId(),
                PaymentGatewayStatus.DONE.name(),
                order.totalAmount()
            )
        );
    }

    private Future<Void> submitWebhook(
        ExecutorService executor,
        CountDownLatch ready,
        CountDownLatch start,
        TossPaymentWebhookRequest request
    ) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            paymentWebhookService.handle("concurrent-transmission", request);
            return null;
        });
    }
}
