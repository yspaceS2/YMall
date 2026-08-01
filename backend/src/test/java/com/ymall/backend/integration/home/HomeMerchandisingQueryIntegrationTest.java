package com.ymall.backend.integration.home;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.home.repository.HomeMerchandisingQueryRepository;
import com.ymall.backend.home.repository.HomeMerchandisingRow;
import com.ymall.backend.home.repository.HomeMerchandisingSection;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HomeMerchandisingQueryIntegrationTest {

    @Autowired private HomeMerchandisingQueryRepository queryRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;

    @Test
    void ranksByNetPaidQuantityAndFallsBackToApprovedProducts() {
        Category fashion = categoryRepository.save(
            new Category("패션", "fashion", null, 1, 1, true)
        );
        Category women = categoryRepository.save(
            new Category("여성패션", "women-fashion", fashion, 2, 1, true)
        );
        Category living = categoryRepository.save(
            new Category("홈 조회 생활", "home-query-living", null, 1, 2, true)
        );
        Product mostlyRefunded = productRepository.save(product(women, "부분 환불 상품"));
        Product netBest = productRepository.save(product(women, "실판매 베스트"));
        Product fullyRefunded = productRepository.save(product(women, "전액 환불 상품"));
        Product canceled = productRepository.save(product(women, "결제 전 취소 상품"));
        Product fallback = productRepository.save(product(living, "판매 이력 없는 신상품"));
        Member member = memberRepository.save(new Member(
            "home-query@ymall.local",
            "encoded-password",
            "홈 조회 회원",
            MemberRole.ROLE_USER
        ));

        createPaidOrder(member, mostlyRefunded, 5, 4, "home-order-1", 1);
        createPaidOrder(member, netBest, 2, 0, "home-order-2", 2);
        createPaidOrder(member, fullyRefunded, 3, 3, "home-order-3", 3);
        createCanceledOrder(member, canceled, "home-order-4");

        List<HomeMerchandisingRow> rows = queryRepository.findMerchandising(
            OffsetDateTime.now(ZoneOffset.UTC).minusDays(30)
        );

        List<HomeMerchandisingRow> categoryBest = rows.stream()
            .filter(row -> row.section() == HomeMerchandisingSection.CATEGORY_BEST)
            .toList();
        assertThat(categoryBest).extracting(HomeMerchandisingRow::productId)
            .contains(netBest.getId(), fallback.getId());
        assertThat(categoryBest.stream()
            .filter(row -> row.groupCategoryId().equals(fashion.getId()))
            .findFirst()
            .orElseThrow()
            .salesQuantity()).isEqualTo(2);

        List<HomeMerchandisingRow> fashionRows = rows.stream()
            .filter(row -> row.section() == HomeMerchandisingSection.FASHION)
            .toList();
        assertThat(fashionRows).extracting(HomeMerchandisingRow::productId)
            .containsExactly(netBest.getId(), mostlyRefunded.getId());
        assertThat(fashionRows).extracting(HomeMerchandisingRow::productId)
            .doesNotContain(fullyRefunded.getId(), canceled.getId());
        assertThat(fashionRows).extracting(HomeMerchandisingRow::salesQuantity)
            .containsExactly(2L, 1L);
    }

    @Test
    void limitsNewArrivalsToFourTwoProductSlides() {
        Category category = categoryRepository.save(
            new Category("신상품 조회", "home-new-arrivals", null, 1, 1, true)
        );
        List<Product> products = new ArrayList<>();
        for (int index = 1; index <= 9; index += 1) {
            products.add(productRepository.save(product(category, "신상품 " + index)));
        }
        productRepository.flush();

        List<HomeMerchandisingRow> newArrivals = queryRepository.findMerchandising(
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(30)
            ).stream()
            .filter(row -> row.section() == HomeMerchandisingSection.NEW_ARRIVAL)
            .toList();

        assertThat(newArrivals).hasSize(8);
        assertThat(newArrivals).extracting(HomeMerchandisingRow::productId)
            .doesNotContain(products.get(0).getId())
            .contains(products.subList(1, 9).stream().map(Product::getId).toArray(Long[]::new));
    }

    private Product product(Category category, String name) {
        return new Product(
            category,
            name,
            "상품 설명",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(4.5),
            10,
            "/images/product.jpg",
            ProductStatus.APPROVED
        );
    }

    private void createPaidOrder(
        Member member,
        Product product,
        int quantity,
        int refundedQuantity,
        String idempotencyKey,
        int approvedDaysAgo
    ) {
        Order order = new Order(member, idempotencyKey);
        OrderItem item = new OrderItem(product, product.getName(), product.getPrice(), quantity);
        order.addItem(item);
        order.completePayment();
        if (refundedQuantity > 0) {
            item.recordRefund(refundedQuantity);
            order.applyRefund(refundedQuantity == quantity);
        }
        orderRepository.saveAndFlush(order);
        paymentRepository.saveAndFlush(Payment.success(
            order,
            idempotencyKey,
            "payment-key-" + idempotencyKey,
            order.getPaymentOrderId(),
            order.getTotalAmount(),
            order.getTotalAmount(),
            "카드",
            OffsetDateTime.now(ZoneOffset.UTC).minusDays(approvedDaysAgo)
        ));
    }

    private void createCanceledOrder(Member member, Product product, String idempotencyKey) {
        Order order = new Order(member, idempotencyKey);
        order.addItem(new OrderItem(product, product.getName(), product.getPrice(), 2));
        order.cancel();
        orderRepository.saveAndFlush(order);
    }
}
