package com.ymall.backend.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.exception.PaymentException;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.refund.entity.PaymentRefundStatus;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentRefundFailureIntegrationTest {

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
    private PaymentRefundRepository refundRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.url",
            () -> "jdbc:h2:mem:ymall-refund-failure;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
        );
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    void recordsFailureAllowsRetryAndBlocksAfterUnknownOutcome() throws Exception {
        Member member = memberRepository.save(new Member(
            "refund-failure@example.com",
            "password",
            "Refund Failure User",
            MemberRole.ROLE_USER
        ));
        Long addressId = memberAddressRepository.save(new MemberAddress(
            member,
            "Home",
            "Recipient",
            "01012345678",
            "00000",
            "123 Test-ro",
            "101",
            true
        )).getId();
        Category category = categoryRepository.save(new Category(
            "Refund failure",
            "refund-failure"
        ));
        Product product = productRepository.save(new Product(
            category,
            "Refund failure product",
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
        String token = jwtTokenProvider.createAccessToken(member).accessToken();

        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"idempotencyKey":"refund-failure-order","addressId":%d}
                    """.formatted(addressId)))
            .andExpect(status().isCreated());
        Order order = orderRepository.findByMemberIdAndIdempotencyKey(
            member.getId(),
            "refund-failure-order"
        ).orElseThrow();
        Long orderItemId = order.getItems().get(0).getId();
        given(paymentGateway.confirm(any())).willReturn(new PaymentGatewayResult(
            "refund-failure-payment-key",
            order.getPaymentOrderId(),
            PaymentGatewayStatus.DONE,
            order.getTotalAmount(),
            BigDecimal.ZERO,
            "CARD",
            OffsetDateTime.now()
        ));
        mockMvc.perform(post(
                "/api/orders/{orderId}/payments/confirmations",
                order.getId()
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "paymentKey":"refund-failure-payment-key",
                      "paymentOrderId":"%s",
                      "amount":20000,
                      "idempotencyKey":"refund-failure-confirmation"
                    }
                    """.formatted(order.getPaymentOrderId())))
            .andExpect(status().isCreated());

        given(paymentGateway.cancel(any()))
            .willThrow(new PaymentException(
                ErrorCode.PAYMENT_GATEWAY_ERROR,
                "REJECT_REFUND",
                "Refund was rejected."
            ))
            .willReturn(new PaymentGatewayResult(
                "refund-failure-payment-key",
                order.getPaymentOrderId(),
                PaymentGatewayStatus.PARTIAL_CANCELED,
                order.getTotalAmount(),
                BigDecimal.valueOf(10000),
                "CARD",
                OffsetDateTime.now()
            ))
            .willReturn(new PaymentGatewayResult(
                "unexpected-payment-key",
                order.getPaymentOrderId(),
                PaymentGatewayStatus.CANCELED,
                order.getTotalAmount(),
                BigDecimal.ZERO,
                "CARD",
                OffsetDateTime.now()
            ));

        mockMvc.perform(post("/api/orders/{orderId}/refunds", order.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(refundJson("failed-refund", orderItemId)))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error.code").value("PAYMENT_GATEWAY_ERROR"));

        assertThat(refundRepository.findAll()).singleElement().satisfies(refund -> {
            assertThat(refund.getStatus()).isEqualTo(PaymentRefundStatus.FAILED);
            assertThat(refund.getFailureCode()).isEqualTo("REJECT_REFUND");
            assertThat(refund.getFailureMessage()).isEqualTo("Refund was rejected.");
        });

        mockMvc.perform(post("/api/orders/{orderId}/refunds", order.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(refundJson("successful-retry", orderItemId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        mockMvc.perform(post("/api/orders/{orderId}/refunds", order.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(refundJson("unknown-refund", orderItemId)))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error.code")
                .value("PAYMENT_REFUND_PROVIDER_MISMATCH"));

        assertThat(refundRepository.findAll())
            .extracting(refund -> refund.getStatus())
            .containsExactlyInAnyOrder(
                PaymentRefundStatus.FAILED,
                PaymentRefundStatus.SUCCEEDED,
                PaymentRefundStatus.UNKNOWN
            );

        mockMvc.perform(post("/api/orders/{orderId}/refunds", order.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(refundJson("blocked-refund", orderItemId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code")
                .value("PAYMENT_REFUND_RECONCILIATION_REQUIRED"));
    }

    private String refundJson(String idempotencyKey, Long orderItemId) {
        return """
            {
              "idempotencyKey":"%s",
              "reason":"Refund integration test",
              "items":[{"orderItemId":%d,"quantity":1}]
            }
            """.formatted(idempotencyKey, orderItemId);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
