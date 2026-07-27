package com.ymall.backend.integration.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.messaging.OrderEventEnvelope;
import com.ymall.backend.global.messaging.OrderEventType;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.refund.entity.PaymentRefund;
import com.ymall.backend.payment.refund.entity.PaymentRefundItem;
import com.ymall.backend.payment.refund.entity.PaymentRefundType;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.repository.SellerProfileRepository;
import com.ymall.backend.settlement.entity.SettlementEntryType;
import com.ymall.backend.settlement.entity.SettlementStatus;
import com.ymall.backend.settlement.repository.SettlementLedgerRepository;
import com.ymall.backend.settlement.service.SettlementLedgerProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SettlementLedgerIntegrationTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-07-27T01:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

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

    @Autowired
    private SettlementLedgerRepository ledgerRepository;

    @Autowired
    private SettlementLedgerProcessor ledgerProcessor;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member seller;
    private Member otherSeller;
    private Member buyer;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        seller = saveMember("ledger-seller@example.com", MemberRole.ROLE_SELLER);
        otherSeller = saveMember("ledger-other@example.com", MemberRole.ROLE_SELLER);
        buyer = saveMember("ledger-buyer@example.com", MemberRole.ROLE_USER);
        SellerProfile sellerProfile = sellerProfileRepository.save(new SellerProfile(
            seller,
            "정산 원장 상점",
            "111-11-11111",
            "정산 원장 테스트"
        ));
        sellerProfileRepository.save(new SellerProfile(
            otherSeller,
            "다른 정산 상점",
            "222-22-22222",
            "다른 판매자"
        ));

        Category category = categoryRepository.save(new Category("정산 상품", "ledger-product"));
        Product product = new Product(
            category,
            "정산 테스트 상품",
            "정산 테스트 상품",
            "YMall",
            new BigDecimal("10000.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            10,
            null,
            ProductStatus.APPROVED
        );
        product.assignSellerProfile(sellerProfile);
        productRepository.save(product);

        order = new Order(buyer, "ledger-order");
        orderItem = new OrderItem(
            product,
            product.getName(),
            product.getPrice(),
            2
        );
        order.addItem(orderItem);
        order.completePayment();
        orderRepository.saveAndFlush(order);
    }

    @Test
    void paymentAndRefundEventsCreateTraceableIdempotentLedgerEntries() {
        OrderEventEnvelope paymentEvent = event(
            OrderEventType.PAYMENT_COMPLETED,
            Map.of()
        );
        ledgerProcessor.process(paymentEvent);
        ledgerProcessor.process(paymentEvent);
        ledgerProcessor.process(event(OrderEventType.PAYMENT_COMPLETED, Map.of()));

        assertThat(ledgerRepository.findAll()).singleElement().satisfies(entry -> {
            assertThat(entry.getEntryType()).isEqualTo(SettlementEntryType.SALE);
            assertThat(entry.getStatus()).isEqualTo(SettlementStatus.PENDING);
            assertThat(entry.getGrossAmount()).isEqualByComparingTo("20000.00");
            assertThat(entry.getFeeAmount()).isEqualByComparingTo("600.00");
            assertThat(entry.getSettlementAmount()).isEqualByComparingTo("19400.00");
            assertThat(entry.getSourceEventId()).isEqualTo(paymentEvent.eventId());
        });

        Payment payment = paymentRepository.save(new Payment(
            order,
            "ledger-payment",
            PaymentResult.SUCCESS,
            null
        ));
        PaymentRefund refund = new PaymentRefund(
            payment,
            order,
            "ledger-refund",
            PaymentRefundType.PARTIAL,
            "부분 환불",
            buyer.getId(),
            MemberRole.ROLE_USER
        );
        refund.addItem(new PaymentRefundItem(orderItem, 1));
        refundRepository.saveAndFlush(refund);

        OrderEventEnvelope refundEvent = event(
            OrderEventType.REFUND_COMPLETED,
            Map.of("refundId", refund.getId())
        );
        ledgerProcessor.process(refundEvent);
        ledgerProcessor.process(refundEvent);

        assertThat(ledgerRepository.findAll()).hasSize(2);
        assertThat(ledgerRepository.findAll()).filteredOn(entry ->
            entry.getEntryType() == SettlementEntryType.REFUND
        ).singleElement().satisfies(entry -> {
            assertThat(entry.getGrossAmount()).isEqualByComparingTo("-10000.00");
            assertThat(entry.getFeeAmount()).isEqualByComparingTo("-300.00");
            assertThat(entry.getSettlementAmount()).isEqualByComparingTo("-9700.00");
            assertThat(entry.getPaymentRefund().getId()).isEqualTo(refund.getId());
        });

        ledgerProcessor.process(event(OrderEventType.ORDER_DELIVERED, Map.of()));

        assertThat(ledgerRepository.findAll()).filteredOn(entry ->
            entry.getEntryType() == SettlementEntryType.SALE
        ).singleElement().extracting(entry -> entry.getStatus())
            .isEqualTo(SettlementStatus.AVAILABLE);
        assertThat(ledgerRepository.findAll())
            .allMatch(entry -> entry.getStatus() == SettlementStatus.AVAILABLE);
    }

    @Test
    void sellerCanQueryOnlyOwnLedgerWithStatusAndDateFilters() throws Exception {
        ledgerProcessor.process(event(OrderEventType.PAYMENT_COMPLETED, Map.of()));
        ledgerProcessor.process(event(OrderEventType.ORDER_DELIVERED, Map.of()));

        mockMvc.perform(get("/api/seller/settlements")
                .header(HttpHeaders.AUTHORIZATION, bearer(seller))
                .queryParam("status", "AVAILABLE")
                .queryParam("from", "2026-07-27T00:00:00Z")
                .queryParam("to", "2026-07-28T00:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].orderId").value(order.getId()))
            .andExpect(jsonPath("$.data.content[0].grossAmount").value(20000.0))
            .andExpect(jsonPath("$.data.content[0].status").value("AVAILABLE"));

        mockMvc.perform(get("/api/seller/settlements")
                .header(HttpHeaders.AUTHORIZATION, bearer(otherSeller)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(0));

        mockMvc.perform(get("/api/seller/settlements")
                .header(HttpHeaders.AUTHORIZATION, bearer(buyer)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/seller/settlements")
                .header(HttpHeaders.AUTHORIZATION, bearer(seller))
                .queryParam("from", "2026-07-28T00:00:00Z")
                .queryParam("to", "2026-07-27T00:00:00Z"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    private OrderEventEnvelope event(OrderEventType type, Map<String, Object> payload) {
        return new OrderEventEnvelope(
            java.util.UUID.randomUUID(),
            type,
            OCCURRED_AT,
            order.getId(),
            buyer.getId(),
            payload,
            OrderEventEnvelope.CURRENT_VERSION
        );
    }

    private Member saveMember(String email, MemberRole role) {
        return memberRepository.save(new Member(
            email,
            passwordEncoder.encode("Test1234!"),
            "정산 테스트 회원",
            role
        ));
    }

    private String bearer(Member member) {
        return "Bearer " + jwtTokenProvider.createAccessToken(member).accessToken();
    }
}
