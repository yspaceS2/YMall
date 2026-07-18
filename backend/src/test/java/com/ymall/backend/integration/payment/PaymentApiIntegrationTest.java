package com.ymall.backend.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private JwtTokenProvider jwtTokenProvider;

    private Product product;
    private Long addressId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(new Member(
            "payment-user@example.com",
            "password",
            "결제 사용자",
            MemberRole.ROLE_USER
        ));
        addressId = memberAddressRepository.save(new MemberAddress(
            member, "Home", "Recipient", "01012345678", "12159", "186 Biryong-ro", "101", true
        )).getId();
        Category category = categoryRepository.save(new Category("결제 상품", "payment-products"));
        product = productRepository.save(new Product(
            category,
            "모의 결제 상품",
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
        accessToken = jwtTokenProvider.createAccessToken(member).accessToken();
    }

    @Test
    void completesPaymentAndReturnsOrderHistory() throws Exception {
        Long orderId = createOrder();

        processPayment(orderId, "payment-success", "SUCCESS")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].orderId").value(orderId))
            .andExpect(jsonPath("$.data.content[0].status").value("PAID"));
    }

    @Test
    void retriesFailedPaymentAndPreventsDuplicateAttempt() throws Exception {
        Long orderId = createOrder();

        processPayment(orderId, "payment-failure", "FAILURE")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.orderStatus").value("PAYMENT_FAILED"))
            .andExpect(jsonPath("$.data.failureMessage").isNotEmpty());
        Long paymentId = paymentRepository.findAll().get(0).getId();

        processPayment(orderId, "payment-failure", "FAILURE")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.paymentId").value(paymentId));

        processPayment(orderId, "payment-retry", "SUCCESS")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        processPayment(orderId, "payment-failure", "FAILURE")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.result").value("FAILURE"))
            .andExpect(jsonPath("$.data.orderStatus").value("PAYMENT_FAILED"));

        assertThat(paymentRepository.findAll()).hasSize(2);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PAID);
    }

    @Test
    void cancelsOrderAndRestoresStockOnce() throws Exception {
        Long orderId = createOrder();
        assertThat(product.getStock()).isEqualTo(8);

        mockMvc.perform(post("/api/orders/{orderId}/cancellations", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CANCELED"));

        assertThat(product.getStock()).isEqualTo(10);

        mockMvc.perform(post("/api/orders/{orderId}/cancellations", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("ORDER_CANCELLATION_NOT_ALLOWED"));
        assertThat(product.getStock()).isEqualTo(10);
    }

    private Long createOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"idempotencyKey":"order-request","addressId":%d}
                    """.formatted(addressId)))
            .andExpect(status().isCreated());
        return orderRepository.findAll().get(0).getId();
    }

    private org.springframework.test.web.servlet.ResultActions processPayment(
        Long orderId,
        String idempotencyKey,
        String result
    ) throws Exception {
        return mockMvc.perform(post("/api/orders/{orderId}/payments", orderId)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"idempotencyKey":"%s","result":"%s"}
                """.formatted(idempotencyKey, result)));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
