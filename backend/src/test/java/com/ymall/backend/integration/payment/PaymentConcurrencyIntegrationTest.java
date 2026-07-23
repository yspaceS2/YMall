package com.ymall.backend.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.repository.CartItemRepository;
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
import com.ymall.backend.payment.dto.PaymentConfirmRequest;
import com.ymall.backend.payment.dto.PaymentResponse;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.payment.service.PaymentService;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@SpringBootTest
@ActiveProfiles("test")
class PaymentConcurrencyIntegrationTest {

    @Autowired
    private PaymentService paymentService;

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

    @MockitoBean
    private PaymentGateway paymentGateway;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.url",
            () -> "jdbc:h2:mem:ymall-payment-concurrency;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
        );
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    void confirmsPaymentOnceForConcurrentRequestsWithSameIdempotencyKey() throws Exception {
        Member member = memberRepository.save(new Member(
            "payment-concurrent@example.com",
            "password",
            "동시 결제 사용자",
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
            "Concurrent payment",
            "concurrent-payment"
        ));
        Product product = productRepository.save(new Product(
            category,
            "Concurrent payment product",
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
            new OrderCreateRequest("concurrent-order", address.getId())
        );
        PaymentConfirmRequest request = new PaymentConfirmRequest(
            "concurrent-payment-key",
            order.paymentOrderId(),
            order.totalAmount(),
            "concurrent-confirmation"
        );
        AtomicInteger gatewayCalls = new AtomicInteger();
        given(paymentGateway.confirm(any())).willAnswer(invocation -> {
            gatewayCalls.incrementAndGet();
            Thread.sleep(250);
            return new PaymentGatewayResult(
                request.paymentKey(),
                request.paymentOrderId(),
                PaymentGatewayStatus.DONE,
                request.amount(),
                BigDecimal.ZERO,
                "CARD",
                OffsetDateTime.now()
            );
        });

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<PaymentResponse>> responses = List.of(
                submitConfirmation(executor, ready, start, member.getId(), order.orderId(), request),
                submitConfirmation(executor, ready, start, member.getId(), order.orderId(), request)
            );

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            PaymentResponse firstResponse = responses.get(0).get(10, TimeUnit.SECONDS);
            PaymentResponse secondResponse = responses.get(1).get(10, TimeUnit.SECONDS);

            assertThat(secondResponse.paymentId()).isEqualTo(firstResponse.paymentId());
        } finally {
            executor.shutdownNow();
        }

        assertThat(gatewayCalls).hasValue(1);
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(orderRepository.findById(order.orderId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PAID);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
            .isEqualTo(8);
    }

    private Future<PaymentResponse> submitConfirmation(
        ExecutorService executor,
        CountDownLatch ready,
        CountDownLatch start,
        Long memberId,
        Long orderId,
        PaymentConfirmRequest request
    ) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            return paymentService.confirmPayment(memberId, orderId, request);
        });
    }
}
