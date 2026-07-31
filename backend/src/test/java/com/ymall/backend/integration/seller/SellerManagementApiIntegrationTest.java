package com.ymall.backend.integration.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.EntityManager;
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

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.DeliveryAddressSnapshot;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.gateway.PaymentGateway;
import com.ymall.backend.payment.gateway.PaymentGatewayResult;
import com.ymall.backend.payment.gateway.PaymentGatewayStatus;
import com.ymall.backend.payment.repository.PaymentRepository;
import com.ymall.backend.payment.refund.repository.PaymentRefundRepository;
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
class SellerManagementApiIntegrationTest {

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
    private PaymentRefundRepository paymentRefundRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PaymentGateway paymentGateway;

    private Member firstSeller;
    private Member secondSeller;
    private Member buyer;
    private SellerProfile firstProfile;
    private SellerProfile secondProfile;
    private Category category;
    private String firstSellerToken;
    private String secondSellerToken;
    private String buyerToken;

    @BeforeEach
    void setUp() {
        firstSeller = saveMember("seller1@example.com", MemberRole.ROLE_SELLER);
        secondSeller = saveMember("seller2@example.com", MemberRole.ROLE_SELLER);
        buyer = saveMember("buyer@example.com", MemberRole.ROLE_USER);
        firstProfile = sellerProfileRepository.save(new SellerProfile(
            firstSeller, "첫 번째 상점", "111-11-11111", "첫 번째 판매자"
        ));
        secondProfile = sellerProfileRepository.save(new SellerProfile(
            secondSeller, "두 번째 상점", "222-22-22222", "두 번째 판매자"
        ));
        category = categoryRepository.save(new Category("전자기기", "electronics"));
        firstSellerToken = token(firstSeller);
        secondSellerToken = token(secondSeller);
        buyerToken = token(buyer);
    }

    @Test
    void sellerCreatesPendingProductOwnedByOwnProfile() throws Exception {
        mockMvc.perform(post("/api/seller/products")
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("판매자 상품")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("판매자 상품"))
            .andExpect(jsonPath("$.data.status").value("PENDING"));

        Product savedProduct = productRepository.findAll().get(0);
        assertThat(savedProduct.getSellerProfile().getId()).isEqualTo(firstProfile.getId());
    }

    @Test
    void sellerCannotUpdateAnotherSellersProduct() throws Exception {
        Product product = saveProduct(firstProfile, "첫 번째 상품");

        mockMvc.perform(put("/api/seller/products/{productId}", product.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(secondSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("변경 시도")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("SELLER_PRODUCT_NOT_FOUND"));

        assertThat(product.getName()).isEqualTo("첫 번째 상품");
    }

    @Test
    void regularMemberCannotAccessSellerApi() throws Exception {
        mockMvc.perform(get("/api/seller/products")
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void sellerFiltersOwnProductsByKeywordCategoryTreeAndStock() throws Exception {
        Category childCategory = categoryRepository.save(new Category(
            "노트북",
            "notebooks",
            category,
            2,
            1,
            true
        ));
        saveProduct(firstProfile, childCategory, "초경량 카메라 노트북", "YTech", 3);
        saveProduct(firstProfile, category, "사무용 모니터", "YDisplay", 8);
        saveProduct(secondProfile, childCategory, "다른 판매자 카메라 노트북", "YTech", 2);

        mockMvc.perform(get("/api/seller/products")
                .param("keyword", "카메라")
                .param("categoryId", category.getId().toString())
                .param("stockCondition", "LTE")
                .param("stockQuantity", "5")
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].name").value("초경량 카메라 노트북"))
            .andExpect(jsonPath("$.data.content[0].stock").value(3));
    }

    @Test
    void sellerSearchesOrdersAndReadsPendingFulfillmentOrderCount() throws Exception {
        Product pendingProduct = saveProduct(firstProfile, "검색 대상 상품");
        Product preparingProduct = saveProduct(firstProfile, "이미 처리한 상품");
        Order pendingOrder = new Order(buyer, "pending-search-order");
        pendingOrder.addItem(new OrderItem(
            pendingProduct,
            pendingProduct.getName(),
            pendingProduct.getPrice(),
            1
        ));
        pendingOrder.addItem(new OrderItem(
            pendingProduct,
            "검색 대상 추가 상품",
            pendingProduct.getPrice(),
            1
        ));
        pendingOrder.completePayment();
        pendingOrder = orderRepository.saveAndFlush(pendingOrder);

        Order preparingOrder = new Order(buyer, "preparing-search-order");
        OrderItem preparingItem = new OrderItem(
            preparingProduct,
            preparingProduct.getName(),
            preparingProduct.getPrice(),
            1
        );
        preparingOrder.addItem(preparingItem);
        preparingOrder.completePayment();
        preparingItem.updateFulfillmentStatus(
            OrderItemFulfillmentStatus.PREPARING,
            null,
            null
        );
        preparingOrder.refreshFulfillmentStatus();
        orderRepository.saveAndFlush(preparingOrder);

        mockMvc.perform(get("/api/seller/orders")
                .param("keyword", "검색 대상")
                .param("fulfillmentStatus", "PENDING")
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].orderId").value(pendingOrder.getId()))
            .andExpect(jsonPath("$.data.content[0].items[0].productName")
                .value("검색 대상 상품"));

        mockMvc.perform(get("/api/seller/orders")
                .param("keyword", pendingOrder.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].orderId").value(pendingOrder.getId()));

        mockMvc.perform(get("/api/seller/orders/pending-count")
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    void normalizesSellerOrderPaginationRange() throws Exception {
        mockMvc.perform(get("/api/seller/orders")
                .param("page", "-1")
                .param("size", "0")
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.size").value(1));

        mockMvc.perform(get("/api/seller/orders")
                .param("size", "1000")
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    void sellerUpdatesOnlyOwnItemsInMixedSellerOrder() throws Exception {
        Product firstProduct = saveProduct(firstProfile, "첫 번째 상품");
        Product secondProduct = saveProduct(secondProfile, "두 번째 상품");
        Order order = new Order(buyer, "seller-order-test");
        order.addItem(new OrderItem(firstProduct, firstProduct.getName(), firstProduct.getPrice(), 1));
        order.addItem(new OrderItem(secondProduct, secondProduct.getName(), secondProduct.getPrice(), 2));
        order.completePayment();
        order = orderRepository.saveAndFlush(order);
        Long orderId = order.getId();

        mockMvc.perform(get("/api/seller/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].orderId").value(orderId))
            .andExpect(jsonPath("$.data.content[0].items.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].items[0].productName").value("첫 번째 상품"));

        mockMvc.perform(patch("/api/seller/orders/{orderId}/status", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fulfillmentStatus\":\"PREPARING\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("PREPARING"))
            .andExpect(jsonPath("$.data.items[0].fulfillmentStatus").value("PREPARING"));

        entityManager.flush();
        entityManager.clear();
        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(updatedOrder.getItems())
            .extracting(OrderItem::getEffectiveFulfillmentStatus)
            .containsExactly(
                OrderItemFulfillmentStatus.PREPARING,
                OrderItemFulfillmentStatus.PENDING
            );
    }

    @Test
    void sellerManagesOwnedOrderItemFulfillmentWithTrackingAndStatusFilter()
        throws Exception {
        Product firstProduct = saveProduct(firstProfile, "개별 출고 상품");
        Product secondProduct = saveProduct(firstProfile, "나중 출고 상품");
        MemberAddress deliveryAddress = new MemberAddress(
            buyer,
            "테스트 배송지",
            "테스트 구매자",
            "01012345678",
            "12345",
            "서울시 테스트로 1",
            "101호",
            true
        );
        Order order = new Order(
            buyer,
            "seller-item-fulfillment",
            new DeliveryAddressSnapshot(deliveryAddress)
        );
        order.addItem(new OrderItem(
            firstProduct,
            firstProduct.getName(),
            firstProduct.getPrice(),
            1
        ));
        order.addItem(new OrderItem(
            secondProduct,
            secondProduct.getName(),
            secondProduct.getPrice(),
            1
        ));
        order.completePayment();
        order = orderRepository.saveAndFlush(order);
        Long orderId = order.getId();
        Long firstItemId = order.getItems().get(0).getId();

        mockMvc.perform(get("/api/seller/orders/{orderId}", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.deliveryAddress.recipientName")
                .value("테스트 구매자"))
            .andExpect(jsonPath("$.data.deliveryAddress.recipientPhone")
                .value("01012345678"))
            .andExpect(jsonPath("$.data.items.length()").value(2));

        mockMvc.perform(patch(
                "/api/seller/orders/{orderId}/items/{orderItemId}/fulfillment",
                orderId,
                firstItemId
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(secondSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fulfillmentStatus\":\"PREPARING\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("SELLER_ORDER_NOT_FOUND"));

        mockMvc.perform(patch(
                "/api/seller/orders/{orderId}/items/{orderItemId}/fulfillment",
                orderId,
                firstItemId
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fulfillmentStatus\":\"PREPARING\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].fulfillmentStatus").value("PREPARING"))
            .andExpect(jsonPath("$.data.items[1].fulfillmentStatus").value("PENDING"));

        mockMvc.perform(patch(
                "/api/seller/orders/{orderId}/items/{orderItemId}/fulfillment",
                orderId,
                firstItemId
            )
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fulfillmentStatus":"SHIPPED",
                      "carrier":"CJ대한통운",
                      "trackingNumber":"1234567890"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("PREPARING"))
            .andExpect(jsonPath("$.data.items[0].fulfillmentStatus").value("SHIPPED"))
            .andExpect(jsonPath("$.data.items[0].carrier").value("CJ대한통운"))
            .andExpect(jsonPath("$.data.items[0].trackingNumber").value("1234567890"))
            .andExpect(jsonPath("$.data.items[0].shippedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.items[1].fulfillmentStatus").value("PENDING"));

        mockMvc.perform(get("/api/seller/orders")
                .param("fulfillmentStatus", "SHIPPED")
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].orderId").value(orderId));

        mockMvc.perform(get("/api/seller/orders")
                .param("fulfillmentStatus", "DELIVERED")
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void sellerShipsOnlyRemainingItemsAfterPartialRefund() throws Exception {
        Product refundedProduct = saveProduct(firstProfile, "Refunded product");
        Product remainingProduct = saveProduct(firstProfile, "Remaining product");
        refundedProduct.decreaseStock(1);
        remainingProduct.decreaseStock(1);
        Order order = new Order(buyer, "partial-refund-fulfillment-order");
        order.addItem(new OrderItem(
            refundedProduct,
            refundedProduct.getName(),
            refundedProduct.getPrice(),
            1
        ));
        order.addItem(new OrderItem(
            remainingProduct,
            remainingProduct.getName(),
            remainingProduct.getPrice(),
            1
        ));
        order.completePayment();
        order = orderRepository.saveAndFlush(order);
        paymentRepository.saveAndFlush(Payment.success(
            order,
            "partial-refund-fulfillment-payment",
            "partial-refund-fulfillment-key",
            order.getPaymentOrderId(),
            order.getTotalAmount(),
            order.getTotalAmount(),
            "CARD",
            OffsetDateTime.now()
        ));
        Long orderId = order.getId();
        Long refundedItemId = order.getItems().get(0).getId();
        given(paymentGateway.cancel(any())).willReturn(new PaymentGatewayResult(
            "partial-refund-fulfillment-key",
            order.getPaymentOrderId(),
            PaymentGatewayStatus.PARTIAL_CANCELED,
            order.getTotalAmount(),
            BigDecimal.valueOf(10000),
            "CARD",
            OffsetDateTime.now()
        ));

        mockMvc.perform(post("/api/seller/orders/{orderId}/refunds", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idempotencyKey":"partial-refund-before-fulfillment",
                      "reason":"Refund one item before fulfillment",
                      "items":[{"orderItemId":%d,"quantity":1}]
                    }
                    """.formatted(refundedItemId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        mockMvc.perform(patch("/api/seller/orders/{orderId}/status", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fulfillmentStatus\":\"PREPARING\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("PREPARING"))
            .andExpect(jsonPath("$.data.items[0].fulfillmentStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.items[1].fulfillmentStatus").value("PREPARING"));

        mockMvc.perform(patch("/api/seller/orders/{orderId}/status", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fulfillmentStatus":"SHIPPED",
                      "carrier":"CJ대한통운",
                      "trackingNumber":"1234567890"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("SHIPPED"));

        mockMvc.perform(patch("/api/seller/orders/{orderId}/status", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fulfillmentStatus\":\"DELIVERED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("DELIVERED"));

        entityManager.flush();
        entityManager.clear();
        Order deliveredOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(deliveredOrder.getItems())
            .extracting(OrderItem::getEffectiveFulfillmentStatus)
            .containsExactly(
                OrderItemFulfillmentStatus.PENDING,
                OrderItemFulfillmentStatus.DELIVERED
            );
    }

    @Test
    void sellerRefundsOnlyOwnedItemsAndAdminRefundsRemainingOrder() throws Exception {
        Product firstProduct = saveProduct(firstProfile, "First seller product");
        Product secondProduct = saveProduct(secondProfile, "Second seller product");
        firstProduct.decreaseStock(1);
        secondProduct.decreaseStock(2);
        Order order = new Order(buyer, "seller-refund-order");
        order.addItem(new OrderItem(
            firstProduct,
            firstProduct.getName(),
            firstProduct.getPrice(),
            1
        ));
        order.addItem(new OrderItem(
            secondProduct,
            secondProduct.getName(),
            secondProduct.getPrice(),
            2
        ));
        order.completePayment();
        order = orderRepository.saveAndFlush(order);
        paymentRepository.saveAndFlush(Payment.success(
            order,
            "seller-refund-payment",
            "seller-refund-payment-key",
            order.getPaymentOrderId(),
            order.getTotalAmount(),
            order.getTotalAmount(),
            "CARD",
            OffsetDateTime.now()
        ));
        Long firstItemId = order.getItems().get(0).getId();
        Long orderId = order.getId();
        given(paymentGateway.cancel(any())).willReturn(
            new PaymentGatewayResult(
                "seller-refund-payment-key",
                order.getPaymentOrderId(),
                PaymentGatewayStatus.PARTIAL_CANCELED,
                order.getTotalAmount(),
                BigDecimal.valueOf(20000),
                "CARD",
                OffsetDateTime.now()
            ),
            new PaymentGatewayResult(
                "seller-refund-payment-key",
                order.getPaymentOrderId(),
                PaymentGatewayStatus.CANCELED,
                order.getTotalAmount(),
                BigDecimal.ZERO,
                "CARD",
                OffsetDateTime.now()
            )
        );

        String firstItemRefund = """
            {
              "idempotencyKey":"seller-item-refund",
              "reason":"Seller approved return",
              "items":[{"orderItemId":%d,"quantity":1}]
            }
            """.formatted(firstItemId);
        mockMvc.perform(post("/api/seller/orders/{orderId}/refunds", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(secondSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstItemRefund))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("PAYMENT_REFUND_AMOUNT_EXCEEDED"));

        mockMvc.perform(post("/api/seller/orders/{orderId}/refunds", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(firstSellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstItemRefund))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].orderItemId").value(firstItemId));

        mockMvc.perform(get("/api/seller/orders/{orderId}/refunds", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(secondSellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));

        Member admin = saveMember("refund-admin@example.com", MemberRole.ROLE_ADMIN);
        mockMvc.perform(post("/api/admin/orders/{orderId}/refunds", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token(admin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "idempotencyKey":"admin-remaining-refund",
                      "reason":"Admin approved remaining refund"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.data.type").value("FULL"))
            .andExpect(jsonPath("$.data.amount").value(20000));

        assertThat(productRepository.findById(firstProduct.getId()).orElseThrow().getStock())
            .isEqualTo(10);
        assertThat(productRepository.findById(secondProduct.getId()).orElseThrow().getStock())
            .isEqualTo(10);
        assertThat(paymentRefundRepository.findAll()).hasSize(2);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
            .isEqualTo(OrderStatus.REFUNDED);
    }

    private Member saveMember(String email, MemberRole role) {
        return memberRepository.save(new Member(email, "password", email, role));
    }

    private Product saveProduct(SellerProfile profile, String name) {
        return saveProduct(profile, category, name, "YMall", 10);
    }

    private Product saveProduct(
        SellerProfile profile,
        Category productCategory,
        String name,
        String brand,
        int stock
    ) {
        Product product = new Product(
            productCategory,
            name,
            "상품 설명",
            brand,
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            stock,
            "thumbnail",
            ProductStatus.APPROVED
        );
        product.assignSellerProfile(profile);
        return productRepository.save(product);
    }

    private String productJson(String name) {
        return """
            {
                "categoryId": %d,
                "name": "%s",
                "description": "상품 설명",
                "brand": "YMall",
                "price": 10000,
                "discountPercentage": 0,
                "discountStartDate": null,
                "discountEndDate": null,
                "freeShipping": true,
                "shippingFee": 0,
                "estimatedDeliveryDays": 3,
                "stock": 10,
                "thumbnailUrl": "thumbnail",
                "images": []
            }
            """.formatted(category.getId(), name);
    }

    private String token(Member member) {
        return jwtTokenProvider.createAccessToken(member).accessToken();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
