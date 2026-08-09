package com.ymall.backend.integration.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.dto.OrderCreateRequest;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.order.service.OrderService;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@SpringBootTest(properties = {
    "spring.datasource.url="
        + "jdbc:h2:mem:order-inventory-redis-fallback-test;"
        + "MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.data.redis.port=1",
    "spring.data.redis.connect-timeout=100ms",
    "spring.data.redis.timeout=100ms"
})
@ActiveProfiles("test")
class OrderInventoryRedisFallbackIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberAddressRepository memberAddressRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderRepository orderRepository;

    @Test
    void commitsOrderAndDatabaseStockWhenRedisEvictionFails() {
        Member member = memberRepository.save(new Member(
            "redis-fallback-order@example.com",
            "password",
            "Redis 장애 주문자",
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
        Category category = categoryRepository.save(
            new Category("Redis 장애 주문", "redis-fallback-order")
        );
        Product product = productRepository.save(new Product(
            category,
            "Redis 장애 재고 상품",
            "description",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            10,
            null,
            ProductStatus.APPROVED
        ));
        cartItemRepository.save(new CartItem(member, product, 2));

        Long orderId = orderService.createOrder(
            member.getId(),
            new OrderCreateRequest("redis-fallback-order", address.getId())
        ).orderId();

        assertThat(orderRepository.findById(orderId)).isPresent();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
            .isEqualTo(8);
    }
}
