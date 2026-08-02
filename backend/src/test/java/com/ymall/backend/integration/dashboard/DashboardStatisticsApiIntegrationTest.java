package com.ymall.backend.integration.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.security.JwtTokenProvider;
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
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerProfileRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardStatisticsApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private MemberRepository memberRepository;
    @Autowired private SellerProfileRepository sellerProfileRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Member sellerMember;
    private Member otherSellerMember;
    private Member adminMember;
    private Member buyerMember;
    private Product sellerProduct;
    private Product otherSellerProduct;

    @BeforeEach
    void setUp() {
        sellerMember = memberRepository.save(member("dashboard-seller@ymall.local", MemberRole.ROLE_SELLER));
        otherSellerMember = memberRepository.save(member("dashboard-other@ymall.local", MemberRole.ROLE_SELLER));
        adminMember = memberRepository.save(member("dashboard-admin@ymall.local", MemberRole.ROLE_ADMIN));
        buyerMember = memberRepository.save(member("dashboard-buyer@ymall.local", MemberRole.ROLE_USER));

        SellerProfile seller = sellerProfileRepository.save(
            new SellerProfile(sellerMember, "대시보드 판매점", "1234567890", "판매자 통계 테스트")
        );
        SellerProfile otherSeller = sellerProfileRepository.save(
            new SellerProfile(otherSellerMember, "다른 판매점", "0987654321", "소유권 테스트")
        );
        Category category = categoryRepository.save(
            new Category("패션", "dashboard-fashion", null, 1, 1, true)
        );
        sellerProduct = productRepository.save(product(category, seller, "판매자 상품"));
        otherSellerProduct = productRepository.save(product(category, otherSeller, "다른 판매자 상품"));

        Order kstBoundaryOrder = createPaidOrder(sellerProduct, 5, 2, "dashboard-order-1");
        moveToKstTodayAtHalfPastMidnight(kstBoundaryOrder);
        createPaidOrder(otherSellerProduct, 2, 0, "dashboard-order-2");
        createPaidOrder(sellerProduct, 4, 4, "dashboard-order-3");
    }

    @Test
    void returnsSellerStatisticsForOwnedProductsOnlyAndSubtractsRefundedQuantity() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard/statistics")
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerMember)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.period.period").value("30d"))
            .andExpect(jsonPath("$.data.period.interval").value("DAY"))
            .andExpect(jsonPath("$.data.trend.length()").value(30))
            .andExpect(jsonPath("$.data.netSalesAmount").value(30000))
            .andExpect(jsonPath("$.data.orderCount").value(2))
            .andExpect(jsonPath("$.data.salesQuantity").value(3))
            .andExpect(jsonPath("$.data.trend[29].salesQuantity").value(3))
            .andExpect(jsonPath("$.data.topProducts[0].productId").value(sellerProduct.getId()))
            .andExpect(jsonPath("$.data.topProducts[0].salesQuantity").value(3))
            .andExpect(jsonPath("$.data.pendingTasks.orders").value(1));
    }

    @Test
    void returnsAdminStatisticsAcrossSellersAndFillsMonthlyBuckets() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .param("period", "6m")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminMember)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.period.interval").value("MONTH"))
            .andExpect(jsonPath("$.data.transactionTrend.length()").value(6))
            .andExpect(jsonPath("$.data.registrationTrend.length()").value(6))
            .andExpect(jsonPath("$.data.netTransactionAmount").value(50000))
            .andExpect(jsonPath("$.data.orderCount").value(3))
            .andExpect(jsonPath("$.data.salesQuantity").value(5))
            .andExpect(jsonPath("$.data.transactionTrend[5].salesQuantity").value(5))
            .andExpect(jsonPath("$.data.registrationTrend[5].members").value(4))
            .andExpect(jsonPath("$.data.registrationTrend[5].sellers").value(2))
            .andExpect(jsonPath("$.data.topProducts.length()").value(2))
            .andExpect(jsonPath("$.data.categorySales[0].netSalesAmount").value(50000));
    }

    @Test
    void enforcesRoleBoundariesAndRejectsUnsupportedPeriods() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard/statistics")
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerMember)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/dashboard/statistics")
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerMember)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/seller/dashboard/statistics")
                .param("period", "90d")
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerMember)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void returnsZeroFilledStatisticsWhenSellerHasNoSalesData() throws Exception {
        Member emptySeller = memberRepository.save(
            member("dashboard-empty@ymall.local", MemberRole.ROLE_SELLER)
        );
        sellerProfileRepository.save(
            new SellerProfile(emptySeller, "Empty Store", "1111111111", "Empty Owner")
        );

        mockMvc.perform(get("/api/seller/dashboard/statistics")
                .param("period", "7d")
                .header(HttpHeaders.AUTHORIZATION, bearer(emptySeller)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.trend.length()").value(7))
            .andExpect(jsonPath("$.data.netSalesAmount").value(0))
            .andExpect(jsonPath("$.data.orderCount").value(0))
            .andExpect(jsonPath("$.data.salesQuantity").value(0))
            .andExpect(jsonPath("$.data.topProducts.length()").value(0))
            .andExpect(jsonPath("$.data.pendingTasks.orders").value(0));
    }

    private Member member(String email, MemberRole role) {
        return new Member(email, "encoded-password", "대시보드 테스트", role);
    }

    private Product product(Category category, SellerProfile seller, String name) {
        Product product = new Product(
            category,
            name,
            "대시보드 통계 상품",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(4.5),
            100,
            "/images/dashboard-product.jpg",
            ProductStatus.APPROVED
        );
        product.assignSellerProfile(seller);
        return product;
    }

    private Order createPaidOrder(
        Product product,
        int quantity,
        int refundedQuantity,
        String idempotencyKey
    ) {
        Order order = new Order(buyerMember, idempotencyKey);
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
            OffsetDateTime.now(ZoneOffset.UTC)
        ));
        return order;
    }

    private void moveToKstTodayAtHalfPastMidnight(Order order) {
        LocalDateTime utcCreatedAt = LocalDate.now(ZoneId.of("Asia/Seoul"))
            .atTime(0, 30)
            .atZone(ZoneId.of("Asia/Seoul"))
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime();
        jdbcTemplate.update(
            "UPDATE orders SET created_at = ? WHERE id = ?",
            utcCreatedAt,
            order.getId()
        );
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member).accessToken();
    }
}
