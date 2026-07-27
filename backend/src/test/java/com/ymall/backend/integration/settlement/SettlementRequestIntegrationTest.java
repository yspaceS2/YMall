package com.ymall.backend.integration.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.entity.SellerSettlementAccount;
import com.ymall.backend.seller.entity.SettlementAccountVerificationStatus;
import com.ymall.backend.seller.repository.SellerProfileRepository;
import com.ymall.backend.seller.repository.SellerSettlementAccountRepository;
import com.ymall.backend.settlement.entity.SettlementRequestStatus;
import com.ymall.backend.settlement.entity.SettlementStatus;
import com.ymall.backend.settlement.repository.SettlementLedgerRepository;
import com.ymall.backend.settlement.repository.SettlementRequestHistoryRepository;
import com.ymall.backend.settlement.repository.SettlementRequestRepository;
import com.ymall.backend.settlement.service.SettlementLedgerProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SettlementRequestIntegrationTest {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @Autowired
    private SellerSettlementAccountRepository settlementAccountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SettlementLedgerRepository ledgerRepository;

    @Autowired
    private SettlementRequestRepository requestRepository;

    @Autowired
    private SettlementRequestHistoryRepository historyRepository;

    @Autowired
    private SettlementLedgerProcessor ledgerProcessor;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member seller;
    private Member buyer;
    private Member admin;
    private Order order;
    private String targetPeriod;
    private Instant occurredAt;

    @BeforeEach
    void setUp() {
        seller = saveMember("settlement-request-seller@example.com", MemberRole.ROLE_SELLER);
        buyer = saveMember("settlement-request-buyer@example.com", MemberRole.ROLE_USER);
        admin = saveMember("settlement-request-admin@example.com", MemberRole.ROLE_ADMIN);
        SellerProfile sellerProfile = sellerProfileRepository.save(new SellerProfile(
            seller,
            "월별 정산 상점",
            "333-33-33333",
            "월별 정산 통합 테스트"
        ));
        settlementAccountRepository.save(new SellerSettlementAccount(
            sellerProfile,
            "004",
            "encrypted-holder",
            "encrypted-account",
            "1234",
            SettlementAccountVerificationStatus.UNVERIFIED,
            Instant.now()
        ));

        Category category = categoryRepository.save(new Category("정산 상품", "settlement-request"));
        Product product = new Product(
            category,
            "월별 정산 상품",
            "월별 정산 상품",
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

        order = new Order(buyer, "settlement-request-order");
        order.addItem(new OrderItem(product, product.getName(), product.getPrice(), 2));
        order.completePayment();
        orderRepository.saveAndFlush(order);

        YearMonth previousMonth = YearMonth.now(BUSINESS_ZONE).minusMonths(1);
        targetPeriod = previousMonth.toString();
        occurredAt = previousMonth.atDay(15).atStartOfDay(BUSINESS_ZONE).toInstant();
        ledgerProcessor.process(event(OrderEventType.PAYMENT_COMPLETED));
        ledgerProcessor.process(event(OrderEventType.ORDER_DELIVERED));
    }

    @Test
    void sellerRequestsAndAdminApprovesThenCompletesMockPayment() throws Exception {
        mockMvc.perform(post("/api/seller/settlement-requests")
                .header(HttpHeaders.AUTHORIZATION, bearer(seller))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"period\":\"" + targetPeriod + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REQUESTED"))
            .andExpect(jsonPath("$.data.settlementAmount").value(19400.0));

        mockMvc.perform(post("/api/seller/settlement-requests")
                .header(HttpHeaders.AUTHORIZATION, bearer(seller))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"period\":\"" + targetPeriod + "\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("SETTLEMENT_REQUEST_DUPLICATED"));

        Long requestId = requestRepository.findAll().get(0).getId();
        mockMvc.perform(patch("/api/admin/settlement-requests/{requestId}/approval", requestId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(patch("/api/admin/settlement-requests/{requestId}/approval", requestId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("SETTLEMENT_REQUEST_STATUS_INVALID"));

        mockMvc.perform(post(
                "/api/admin/settlement-requests/{requestId}/mock-payments",
                requestId
            ).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PAID"))
            .andExpect(jsonPath("$.data.mockPaymentReference").value(
                org.hamcrest.Matchers.startsWith("MOCK-")
            ));

        assertThat(ledgerRepository.findAll()).allSatisfy(entry -> {
            assertThat(entry.getStatus()).isEqualTo(SettlementStatus.PAID);
            assertThat(entry.getSettlementRequest().getId()).isEqualTo(requestId);
        });
        assertThat(historyRepository
            .findAllBySettlementRequestIdOrderByCreatedAtAsc(requestId))
            .extracting(history -> history.getToStatus())
            .containsExactly(
                SettlementRequestStatus.REQUESTED,
                SettlementRequestStatus.APPROVED,
                SettlementRequestStatus.PAID
            );
    }

    @Test
    void rejectionReleasesLedgerAndAllowsSamePeriodResubmission() throws Exception {
        mockMvc.perform(post("/api/seller/settlement-requests")
                .header(HttpHeaders.AUTHORIZATION, bearer(seller))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"period\":\"" + targetPeriod + "\"}"))
            .andExpect(status().isOk());
        Long requestId = requestRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/api/admin/settlement-requests/{requestId}/rejection", requestId)
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"계좌 정보를 다시 확인해 주세요.\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REJECTED"))
            .andExpect(jsonPath("$.data.rejectionReason").value(
                "계좌 정보를 다시 확인해 주세요."
            ));

        assertThat(ledgerRepository.findAll()).allSatisfy(entry -> {
            assertThat(entry.getStatus()).isEqualTo(SettlementStatus.AVAILABLE);
            assertThat(entry.getSettlementRequest()).isNull();
        });

        mockMvc.perform(post("/api/seller/settlement-requests")
                .header(HttpHeaders.AUTHORIZATION, bearer(seller))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"period\":\"" + targetPeriod + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.settlementRequestId").value(requestId))
            .andExpect(jsonPath("$.data.status").value("REQUESTED"));

        assertThat(requestRepository.findAll()).hasSize(1);
    }

    @Test
    void permissionsAndClosedPeriodAreEnforced() throws Exception {
        mockMvc.perform(post("/api/seller/settlement-requests")
                .header(HttpHeaders.AUTHORIZATION, bearer(buyer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"period\":\"" + targetPeriod + "\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/settlement-requests")
                .header(HttpHeaders.AUTHORIZATION, bearer(seller)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/seller/settlement-requests")
                .header(HttpHeaders.AUTHORIZATION, bearer(seller))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"period\":\"" + YearMonth.now(BUSINESS_ZONE) + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("SETTLEMENT_REQUEST_PERIOD_INVALID"));
    }

    private OrderEventEnvelope event(OrderEventType type) {
        return new OrderEventEnvelope(
            java.util.UUID.randomUUID(),
            type,
            occurredAt,
            order.getId(),
            buyer.getId(),
            Map.of(),
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
