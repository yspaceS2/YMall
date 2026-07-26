package com.ymall.backend.integration.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.global.config.ProductCacheNames;
import com.ymall.backend.global.messaging.outbox.OrderOutboxEventRepository;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.dto.OrderCreateRequest;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.order.service.OrderService;
import com.ymall.backend.order.service.PendingOrderExpirationService;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.service.ProductService;

@SpringBootTest(properties = {
    "spring.datasource.url="
        + "jdbc:h2:mem:order-inventory-cache-test;"
        + "MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@ActiveProfiles("test")
class OrderInventoryCacheIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private PendingOrderExpirationService expirationService;
    @Autowired private ProductService productService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberAddressRepository memberAddressRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderOutboxEventRepository outboxEventRepository;
    @Autowired private CacheManager cacheManager;

    private Member member;
    private MemberAddress address;
    private Product product;

    @BeforeEach
    void setUp() {
        clearProductCache();
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        memberAddressRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();

        member = memberRepository.save(new Member(
            "inventory-cache@example.com",
            "password",
            "재고 캐시 사용자",
            MemberRole.ROLE_USER
        ));
        address = memberAddressRepository.save(new MemberAddress(
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
            new Category("재고 캐시", "inventory-cache")
        );
        product = productRepository.save(new Product(
            category,
            "재고 캐시 상품",
            "description",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            10,
            null,
            ProductStatus.APPROVED
        ));
    }

    @Test
    void evictsCachedStockAfterOrderCreationAndExpiration() {
        assertThat(productService.getProduct(product.getId()).stock()).isEqualTo(10);
        assertThat(productCache().get(product.getId())).isNotNull();
        cartItemRepository.save(new CartItem(member, product, 2));

        Long orderId = orderService.createOrder(
            member.getId(),
            new OrderCreateRequest("inventory-cache-order", address.getId())
        ).orderId();

        assertThat(productCache().get(product.getId())).isNull();
        assertThat(productService.getProduct(product.getId()).stock()).isEqualTo(8);

        int expiredCount = expirationService.expireCreatedOnOrBefore(
            LocalDateTime.now().plusMinutes(1)
        );

        assertThat(expiredCount).isEqualTo(1);
        assertThat(productCache().get(product.getId())).isNull();
        assertThat(productService.getProduct(product.getId()).stock()).isEqualTo(10);
        Order expiredOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(expiredOrder.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(expiredOrder.isInventoryReserved()).isFalse();
    }

    private org.springframework.cache.Cache productCache() {
        return cacheManager.getCache(ProductCacheNames.DETAILS);
    }

    private void clearProductCache() {
        org.springframework.cache.Cache cache =
            cacheManager.getCache(ProductCacheNames.DETAILS);
        if (cache != null) {
            cache.clear();
        }
    }
}
