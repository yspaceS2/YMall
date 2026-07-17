package com.ymall.backend.integration.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.dto.OrderCreateRequest;
import com.ymall.backend.order.dto.OrderResponse;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.order.service.OrderService;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.url",
            () -> "jdbc:h2:mem:ymall-order-concurrency;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
        );
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    void createsSingleOrderForConcurrentRequestsWithSameIdempotencyKey() throws Exception {
        Member member = memberRepository.save(new Member(
            "concurrent-user@example.com",
            "password",
            "동시 주문 사용자",
            MemberRole.ROLE_USER
        ));
        Category category = categoryRepository.save(new Category("전자기기", "concurrent-electronics"));
        Product product = productRepository.save(new Product(
            category,
            "동시 주문 상품",
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

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<OrderResponse>> responses = List.of(
                submitOrder(executor, ready, start, member.getId()),
                submitOrder(executor, ready, start, member.getId())
            );

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            OrderResponse firstResponse = responses.get(0).get(10, TimeUnit.SECONDS);
            OrderResponse secondResponse = responses.get(1).get(10, TimeUnit.SECONDS);

            assertThat(secondResponse.orderId()).isEqualTo(firstResponse.orderId());
        } finally {
            executor.shutdownNow();
        }

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(8);
        assertThat(cartItemRepository.findAll()).isEmpty();
    }

    private Future<OrderResponse> submitOrder(
        ExecutorService executor,
        CountDownLatch ready,
        CountDownLatch start,
        Long memberId
    ) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            return orderService.createOrder(memberId, new OrderCreateRequest("concurrent-request"));
        });
    }
}
