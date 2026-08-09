package com.ymall.backend.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.PaymentResult;
import com.ymall.backend.payment.exception.PaymentException;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.payment.webhook.entity.PaymentWebhookProcessingResult;
import com.ymall.backend.payment.webhook.repository.PaymentWebhookEventRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentWebhookIntegrationTest {

    private static final String PAYMENT_KEY = "webhook-payment-key";

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
    private PaymentWebhookEventRepository webhookEventRepository;

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
            "webhook-user"
        );
        Member member = fixture.member();
        addressId = fixture.address().getId();
        product = fixture.product();
        accessToken = jwtTokenProvider.createAccessToken(member).accessToken();
    }

    @Test
    void appliesVerifiedPaymentWhenBrowserCallbackWasMissed() throws Exception {
        Order order = createOrder();
        given(paymentGateway.findByPaymentKey(PAYMENT_KEY))
            .willReturn(gatewayResult(order, PaymentGatewayStatus.DONE));

        sendWebhook("transmission-done", order, PaymentGatewayStatus.DONE)
            .andExpect(status().isOk());

        Order synchronizedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(synchronizedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentRepository.findByPaymentKey(PAYMENT_KEY))
            .get()
            .satisfies(payment -> {
                assertThat(payment.getResult()).isEqualTo(PaymentResult.SUCCESS);
                assertThat(payment.getProviderStatus()).isEqualTo(PaymentGatewayStatus.DONE);
            });
        assertThat(webhookEventRepository.findAll())
            .singleElement()
            .satisfies(event ->
                assertThat(event.getResult()).isEqualTo(PaymentWebhookProcessingResult.APPLIED)
            );
    }

    @Test
    void processesSameTransmissionOnlyOnce() throws Exception {
        Order order = createOrder();
        given(paymentGateway.findByPaymentKey(PAYMENT_KEY))
            .willReturn(gatewayResult(order, PaymentGatewayStatus.DONE));

        sendWebhook("transmission-duplicate", order, PaymentGatewayStatus.DONE)
            .andExpect(status().isOk());
        sendWebhook("transmission-duplicate", order, PaymentGatewayStatus.DONE)
            .andExpect(status().isOk());

        assertThat(paymentRepository.findAll()).hasSize(1);
        assertThat(webhookEventRepository.findAll()).hasSize(1);
        then(paymentGateway).should(times(1)).findByPaymentKey(PAYMENT_KEY);
    }

    @Test
    void rejectsWebhookWhenVerifiedPaymentDoesNotMatchPayload() throws Exception {
        Order order = createOrder();
        PaymentGatewayResult invalidResult = new PaymentGatewayResult(
            PAYMENT_KEY,
            order.getPaymentOrderId(),
            PaymentGatewayStatus.DONE,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            "CARD",
            OffsetDateTime.now()
        );
        given(paymentGateway.findByPaymentKey(PAYMENT_KEY)).willReturn(invalidResult);

        sendWebhook("transmission-invalid", order, PaymentGatewayStatus.DONE)
            .andExpect(status().isBadRequest());

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(paymentRepository.findAll()).isEmpty();
        assertThat(webhookEventRepository.findAll()).isEmpty();
    }

    @Test
    void usesCurrentProviderStatusForOutOfOrderWebhook() throws Exception {
        Order order = createOrder();
        given(paymentGateway.findByPaymentKey(PAYMENT_KEY))
            .willReturn(gatewayResult(order, PaymentGatewayStatus.DONE));

        sendWebhook("transmission-stale", order, PaymentGatewayStatus.ABORTED)
            .andExpect(status().isOk());

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PAID);
        assertThat(webhookEventRepository.findAll())
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getRequestedStatus()).isEqualTo(PaymentGatewayStatus.ABORTED);
                assertThat(event.getVerifiedStatus()).isEqualTo(PaymentGatewayStatus.DONE);
                assertThat(event.getResult()).isEqualTo(PaymentWebhookProcessingResult.STALE_EVENT);
            });
    }

    @Test
    void retriesSameTransmissionAfterGatewayFailure() throws Exception {
        Order order = createOrder();
        given(paymentGateway.findByPaymentKey(PAYMENT_KEY))
            .willThrow(new PaymentException(
                ErrorCode.PAYMENT_GATEWAY_ERROR,
                "TEMPORARY_ERROR",
                "Temporary gateway error."
            ))
            .willReturn(gatewayResult(order, PaymentGatewayStatus.DONE));

        sendWebhook("transmission-retry", order, PaymentGatewayStatus.DONE)
            .andExpect(status().isBadGateway());
        assertThat(webhookEventRepository.findAll()).isEmpty();

        sendWebhook("transmission-retry", order, PaymentGatewayStatus.DONE)
            .andExpect(status().isOk());

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PAID);
        assertThat(webhookEventRepository.findAll()).hasSize(1);
    }

    @Test
    void appliesVerifiedFailureAndRestoresReservedStock() throws Exception {
        Order order = createOrder();
        given(paymentGateway.findByPaymentKey(PAYMENT_KEY))
            .willReturn(gatewayResult(order, PaymentGatewayStatus.ABORTED));

        sendWebhook("transmission-aborted", order, PaymentGatewayStatus.ABORTED)
            .andExpect(status().isOk());

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
            .isEqualTo(10);
        assertThat(paymentRepository.findByPaymentKey(PAYMENT_KEY))
            .get()
            .satisfies(payment -> {
                assertThat(payment.getResult()).isEqualTo(PaymentResult.FAILURE);
                assertThat(payment.getProviderStatus()).isEqualTo(PaymentGatewayStatus.ABORTED);
            });
    }

    @Test
    void appliesVerifiedFullCancellationWithoutRestoringStockTwice() throws Exception {
        Order order = createOrder();
        given(paymentGateway.findByPaymentKey(PAYMENT_KEY))
            .willReturn(gatewayResult(order, PaymentGatewayStatus.DONE))
            .willReturn(gatewayResult(order, PaymentGatewayStatus.CANCELED));

        sendWebhook("transmission-approved", order, PaymentGatewayStatus.DONE)
            .andExpect(status().isOk());
        sendWebhook("transmission-canceled", order, PaymentGatewayStatus.CANCELED)
            .andExpect(status().isOk());
        sendWebhook("transmission-canceled", order, PaymentGatewayStatus.CANCELED)
            .andExpect(status().isOk());

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.CANCELED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock())
            .isEqualTo(10);
        assertThat(paymentRepository.findByPaymentKey(PAYMENT_KEY))
            .get()
            .extracting(payment -> payment.getProviderStatus())
            .isEqualTo(PaymentGatewayStatus.CANCELED);
        assertThat(webhookEventRepository.findAll()).hasSize(2);
    }

    private Order createOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"idempotencyKey":"webhook-order","addressId":%d}
                    """.formatted(addressId)))
            .andExpect(status().isCreated());
        return orderRepository.findAll().get(0);
    }

    private PaymentGatewayResult gatewayResult(
        Order order,
        PaymentGatewayStatus status
    ) {
        return new PaymentGatewayResult(
            PAYMENT_KEY,
            order.getPaymentOrderId(),
            status,
            order.getTotalAmount(),
            status == PaymentGatewayStatus.CANCELED
                ? BigDecimal.ZERO
                : order.getTotalAmount(),
            "CARD",
            OffsetDateTime.now()
        );
    }

    private org.springframework.test.web.servlet.ResultActions sendWebhook(
        String transmissionId,
        Order order,
        PaymentGatewayStatus requestedStatus
    ) throws Exception {
        return mockMvc.perform(post("/api/payments/webhooks/toss")
            .header("tosspayments-webhook-transmission-id", transmissionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "eventType":"PAYMENT_STATUS_CHANGED",
                  "createdAt":"2026-07-24T12:00:00",
                  "data":{
                    "paymentKey":"%s",
                    "orderId":"%s",
                    "status":"%s",
                    "totalAmount":%s
                  }
                }
                """.formatted(
                    PAYMENT_KEY,
                    order.getPaymentOrderId(),
                    requestedStatus.name(),
                    order.getTotalAmount().toPlainString()
                )));
    }
}
