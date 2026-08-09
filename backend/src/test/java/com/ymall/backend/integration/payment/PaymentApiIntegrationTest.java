package com.ymall.backend.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.exception.PaymentException;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.product.entity.Product;
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
    private PaymentRefundRepository paymentRefundRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private PaymentGateway paymentGateway;

    private Product product;
    private Long addressId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        PaymentTestFixture.PaymentOrderData fixture = PaymentTestFixture.createOrderData(
            memberRepository,
            memberAddressRepository,
            categoryRepository,
            productRepository,
            cartItemRepository,
            "payment-user"
        );
        Member member = fixture.member();
        addressId = fixture.address().getId();
        product = fixture.product();
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
            .andExpect(jsonPath("$.data.status").value("PAID"))
            .andExpect(jsonPath("$.data.refundSupported").value(false));

        mockMvc.perform(get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].orderId").value(orderId))
            .andExpect(jsonPath("$.data.content[0].status").value("PAID"))
            .andExpect(jsonPath("$.data.content[0].refundSupported").value(false));
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

    @Test
    void confirmsTossPaymentWithServerOrderAmount() throws Exception {
        Long orderId = createOrder();
        Order order = orderRepository.findById(orderId).orElseThrow();
        given(paymentGateway.confirm(any())).willReturn(new PaymentGatewayResult(
            "toss-payment-key",
            order.getPaymentOrderId(),
            PaymentGatewayStatus.DONE,
            order.getTotalAmount(),
            BigDecimal.ZERO,
            "CARD",
            OffsetDateTime.now()
        ));

        confirmPayment(order, "toss-payment-key", "confirmation-request")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.paymentKey").value("toss-payment-key"))
            .andExpect(jsonPath("$.data.paymentOrderId").value(order.getPaymentOrderId()))
            .andExpect(jsonPath("$.data.requestedAmount").value(20000))
            .andExpect(jsonPath("$.data.approvedAmount").value(20000))
            .andExpect(jsonPath("$.data.result").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PAID);
        assertThat(paymentRepository.findAll()).hasSize(1);

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refundSupported").value(true));
    }

    @Test
    void rejectsChangedAmountBeforeCallingGateway() throws Exception {
        Long orderId = createOrder();
        Order order = orderRepository.findById(orderId).orElseThrow();

        mockMvc.perform(post("/api/orders/{orderId}/payments/confirmations", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "paymentKey":"toss-payment-key",
                      "paymentOrderId":"%s",
                      "amount":1,
                      "idempotencyKey":"confirmation-request"
                    }
                    """.formatted(order.getPaymentOrderId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("PAYMENT_AMOUNT_MISMATCH"));

        assertThat(paymentRepository.findAll()).isEmpty();
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void restoresStockAfterGatewayFailureAndReservesItAgainOnRetry() throws Exception {
        Long orderId = createOrder();
        Order order = orderRepository.findById(orderId).orElseThrow();
        given(paymentGateway.confirm(any()))
            .willThrow(new PaymentException(
                ErrorCode.PAYMENT_GATEWAY_ERROR,
                "REJECT_CARD_PAYMENT",
                "Card payment was rejected."
            ))
            .willReturn(new PaymentGatewayResult(
                "retry-payment-key",
                order.getPaymentOrderId(),
                PaymentGatewayStatus.DONE,
                order.getTotalAmount(),
                BigDecimal.ZERO,
                "CARD",
                OffsetDateTime.now()
            ));

        confirmPayment(order, "failed-payment-key", "failed-confirmation")
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error.code").value("PAYMENT_GATEWAY_ERROR"));

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
            .isEqualTo(10);
        Order failedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(failedOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(failedOrder.isInventoryReserved()).isFalse();

        confirmPayment(order, "retry-payment-key", "retry-confirmation")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.result").value("SUCCESS"));

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
            .isEqualTo(8);
        Order paidOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paidOrder.isInventoryReserved()).isTrue();
        assertThat(paymentRepository.findAll()).hasSize(2);
    }

    @Test
    void processesPartialAndRemainingRefundIdempotently() throws Exception {
        Long orderId = createOrder();
        Order order = orderRepository.findById(orderId).orElseThrow();
        given(paymentGateway.confirm(any())).willReturn(new PaymentGatewayResult(
            "refund-payment-key",
            order.getPaymentOrderId(),
            PaymentGatewayStatus.DONE,
            order.getTotalAmount(),
            BigDecimal.ZERO,
            "CARD",
            OffsetDateTime.now()
        ));
        confirmPayment(order, "refund-payment-key", "refund-confirmation")
            .andExpect(status().isCreated());

        Long orderItemId = order.getItems().get(0).getId();
        given(paymentGateway.cancel(any()))
            .willReturn(
                new PaymentGatewayResult(
                    "refund-payment-key",
                    order.getPaymentOrderId(),
                    PaymentGatewayStatus.PARTIAL_CANCELED,
                    order.getTotalAmount(),
                    BigDecimal.valueOf(10000),
                    "CARD",
                    OffsetDateTime.now()
                ),
                new PaymentGatewayResult(
                    "refund-payment-key",
                    order.getPaymentOrderId(),
                    PaymentGatewayStatus.CANCELED,
                    order.getTotalAmount(),
                    BigDecimal.ZERO,
                    "CARD",
                    OffsetDateTime.now()
                )
            );

        String partialRequest = """
            {
              "idempotencyKey":"partial-refund",
              "reason":"Changed my mind",
              "items":[{"orderItemId":%d,"quantity":1}]
            }
            """.formatted(orderItemId);
        mockMvc.perform(post("/api/orders/{orderId}/refunds", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(partialRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.data.type").value("PARTIAL"))
            .andExpect(jsonPath("$.data.amount").value(10000));

        mockMvc.perform(post("/api/orders/{orderId}/refunds", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(partialRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PARTIALLY_REFUNDED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
            .isEqualTo(9);
        verify(paymentGateway, times(1)).cancel(any());

        mockMvc.perform(post("/api/orders/{orderId}/refunds", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idempotencyKey":"remaining-refund",
                      "reason":"Cancel remaining quantity"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.data.type").value("FULL"))
            .andExpect(jsonPath("$.data.amount").value(10000));

        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.REFUNDED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
            .isEqualTo(10);
        assertThat(paymentRefundRepository.findAll()).hasSize(2);
        verify(paymentGateway, times(2)).cancel(any());

        mockMvc.perform(get("/api/orders/{orderId}/refunds", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));
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

    private org.springframework.test.web.servlet.ResultActions confirmPayment(
        Order order,
        String paymentKey,
        String idempotencyKey
    ) throws Exception {
        return mockMvc.perform(post(
                "/api/orders/{orderId}/payments/confirmations",
                order.getId()
            )
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "paymentKey":"%s",
                  "paymentOrderId":"%s",
                  "amount":%s,
                  "idempotencyKey":"%s"
                }
                """.formatted(
                    paymentKey,
                    order.getPaymentOrderId(),
                    order.getTotalAmount().toPlainString(),
                    idempotencyKey
                )));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
