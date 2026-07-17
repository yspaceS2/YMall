package com.ymall.backend.integration.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.notification.entity.NotificationType;
import com.ymall.backend.notification.repository.NotificationRepository;
import com.ymall.backend.order.repository.OrderRepository;
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
class NotificationApiIntegrationTest {

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
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String buyerToken;
    private String otherToken;
    private String sellerToken;

    @BeforeEach
    void setUp() {
        Member buyer = saveMember("notification-buyer@example.com", MemberRole.ROLE_USER);
        Member other = saveMember("notification-other@example.com", MemberRole.ROLE_USER);
        Member seller = saveMember("notification-seller@example.com", MemberRole.ROLE_SELLER);
        SellerProfile sellerProfile = sellerProfileRepository.save(new SellerProfile(
            seller,
            "알림 상점",
            "333-33-33333",
            "알림 통합 테스트 판매자"
        ));
        Category category = categoryRepository.save(new Category("알림 상품", "notification-products"));
        Product product = new Product(
            category,
            "알림 테스트 상품",
            "상품 설명",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            10,
            "thumbnail",
            ProductStatus.APPROVED
        );
        product.assignSellerProfile(sellerProfile);
        productRepository.save(product);
        cartItemRepository.save(new CartItem(buyer, product, 1));
        buyerToken = token(buyer);
        otherToken = token(other);
        sellerToken = token(seller);
    }

    @Test
    void recordsOrderPaymentAndFulfillmentEventsAndProtectsOwnership() throws Exception {
        createOrder();
        Long orderId = orderRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/orders/{orderId}/payments", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"idempotencyKey":"notification-payment","result":"SUCCESS"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/seller/orders/{orderId}/status", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fulfillmentStatus\":\"PREPARING\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(3))
            .andExpect(jsonPath("$.data.content[0].type").value("ORDER_PREPARING"))
            .andExpect(jsonPath("$.data.content[1].type").value("PAYMENT_COMPLETED"))
            .andExpect(jsonPath("$.data.content[2].type").value("ORDER_CREATED"));

        mockMvc.perform(get("/api/notifications")
                .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(0));

        Long notificationId = notificationRepository.findAll().stream()
            .filter(notification -> notification.getType() == NotificationType.ORDER_CREATED)
            .findFirst()
            .orElseThrow()
            .getId();

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("NOTIFICATION_NOT_FOUND"));

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        mockMvc.perform(get("/api/notifications/unread-count")
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unreadCount").value(2));

        mockMvc.perform(patch("/api/notifications/read-all")
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.updatedCount").value(2));

        mockMvc.perform(get("/api/notifications/unread-count")
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    private void createOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"idempotencyKey":"notification-order"}
                    """))
            .andExpect(status().isCreated());
    }

    private Member saveMember(String email, MemberRole role) {
        return memberRepository.save(new Member(email, "password", email, role));
    }

    private String token(Member member) {
        return jwtTokenProvider.createAccessToken(member).accessToken();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
