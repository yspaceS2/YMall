package com.ymall.backend.integration.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.product.service.ProductCacheInvalidator;
import com.ymall.backend.review.repository.ReviewRepository;
import com.ymall.backend.review.event.ReviewSummaryRefreshEvent;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@RecordApplicationEvents
class ReviewApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockitoSpyBean
    private ProductCacheInvalidator productCacheInvalidator;

    private Member buyer;
    private Member otherMember;
    private Product product;
    private OrderItem deliveredOrderItem;
    private String buyerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        buyer = memberRepository.save(new Member(
            "buyer@example.com",
            "password",
            "구매자",
            MemberRole.ROLE_USER
        ));
        otherMember = memberRepository.save(new Member(
            "other@example.com",
            "password",
            "다른 회원",
            MemberRole.ROLE_USER
        ));
        Category category = categoryRepository.save(new Category("전자기기", "electronics"));
        product = productRepository.save(new Product(
            category,
            "무선 키보드",
            "상품 설명",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            null,
            10,
            "thumbnail",
            ProductStatus.APPROVED
        ));
        deliveredOrderItem = saveOrderItem(buyer, product, true, "delivered-order");
        buyerToken = token(buyer);
        otherToken = token(otherMember);
    }

    @Test
    void deliveredBuyerCreatesReviewAndProductReviewsArePublic() throws Exception {
        createReview(buyerToken, deliveredOrderItem.getId(), 5, "아주 만족합니다.")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.rating").value(5))
            .andExpect(jsonPath("$.data.authorName").value("구매자"));

        verify(productCacheInvalidator).evictDetail(product.getId());
        assertThat(applicationEvents.stream(ReviewSummaryRefreshEvent.class)).hasSize(1);

        mockMvc.perform(get("/api/products/{productId}/reviews", product.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].content").value("아주 만족합니다."));

        entityManager.flush();
        entityManager.clear();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getRating())
            .isEqualByComparingTo("5.00");
    }

    @Test
    void rejectsReviewForUndeliveredOrderItemAndDuplicateReview() throws Exception {
        OrderItem pendingOrderItem = saveOrderItem(buyer, product, false, "pending-order");

        createReview(buyerToken, pendingOrderItem.getId(), 4, "아직 배송 전")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_ALLOWED"));

        createReview(buyerToken, deliveredOrderItem.getId(), 5, "첫 리뷰")
            .andExpect(status().isCreated());
        createReview(buyerToken, deliveredOrderItem.getId(), 3, "중복 리뷰")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("REVIEW_ALREADY_EXISTS"));
    }

    @Test
    void onlyOwnerUpdatesAndDeletesReviewAndRatingIsRecalculated() throws Exception {
        createReview(buyerToken, deliveredOrderItem.getId(), 5, "첫 리뷰")
            .andExpect(status().isCreated());
        Long reviewId = reviewRepository.findAll().get(0).getId();

        mockMvc.perform(put("/api/reviews/{reviewId}", reviewId)
                .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson(null, 1, "가로채기")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_FOUND"));

        mockMvc.perform(put("/api/reviews/{reviewId}", reviewId)
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson(null, 3, "수정한 리뷰")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.rating").value(3));

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
            .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerToken)))
            .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        assertThat(reviewRepository.findAll()).isEmpty();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getRating()).isNull();
    }

    @Test
    void returnsOwnReviewsAndValidatesRatingRange() throws Exception {
        createReview(buyerToken, deliveredOrderItem.getId(), 5, "내 리뷰")
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/reviews/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(buyerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].orderItemId").value(deliveredOrderItem.getId()));

        OrderItem anotherItem = saveOrderItem(buyer, product, true, "invalid-rating-order");
        createReview(buyerToken, anotherItem.getId(), 6, "잘못된 평점")
            .andExpect(status().isBadRequest());
    }

    private OrderItem saveOrderItem(Member member, Product targetProduct, boolean delivered, String key) {
        Order order = new Order(member, key);
        OrderItem orderItem = new OrderItem(
            targetProduct,
            targetProduct.getName(),
            targetProduct.getPrice(),
            1
        );
        order.addItem(orderItem);
        order.completePayment();
        if (delivered) {
            orderItem.updateFulfillmentStatus(
                OrderItemFulfillmentStatus.PREPARING,
                null,
                null
            );
            orderItem.updateFulfillmentStatus(
                OrderItemFulfillmentStatus.SHIPPED,
                "CJ대한통운",
                "1234567890"
            );
            orderItem.updateFulfillmentStatus(
                OrderItemFulfillmentStatus.DELIVERED,
                null,
                null
            );
            order.refreshFulfillmentStatus();
        }
        orderRepository.saveAndFlush(order);
        return orderItem;
    }

    private org.springframework.test.web.servlet.ResultActions createReview(
        String accessToken,
        Long orderItemId,
        int rating,
        String content
    ) throws Exception {
        return mockMvc.perform(post("/api/reviews")
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(reviewJson(orderItemId, rating, content)));
    }

    private String reviewJson(Long orderItemId, int rating, String content) {
        String orderItemField = orderItemId == null ? "" : "\"orderItemId\": %d,".formatted(orderItemId);
        return """
            {
                %s
                "rating": %d,
                "content": "%s"
            }
            """.formatted(orderItemField, rating, content);
    }

    private String token(Member member) {
        return jwtTokenProvider.createAccessToken(member).accessToken();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
