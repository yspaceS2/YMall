package com.ymall.backend.integration.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

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
import com.ymall.backend.review.dto.ReviewSummaryResponse;
import com.ymall.backend.review.entity.Review;
import com.ymall.backend.review.repository.ReviewRepository;
import com.ymall.backend.review.repository.ReviewSummaryRepository;
import com.ymall.backend.review.service.ReviewSummaryGenerator;
import com.ymall.backend.review.service.ReviewSummaryService;

@SpringBootTest(properties = "ymall.ai.review-summary.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewSummaryIntegrationTest {

    @Autowired
    private ReviewSummaryService reviewSummaryService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewSummaryRepository reviewSummaryRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ReviewSummaryGenerator generator;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private CacheManager cacheManager;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @MockitoBean
    private Cache cache;

    private Member member;
    private Product product;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);
        when(cacheManager.getCache(anyString())).thenReturn(cache);

        member = memberRepository.save(new Member(
            "summary-buyer@example.com",
            "password",
            "요약 구매자",
            MemberRole.ROLE_USER
        ));
        Category category = categoryRepository.save(new Category("키보드", "summary-keyboard"));
        product = productRepository.save(new Product(
            category,
            "요약 키보드",
            "리뷰 요약 테스트 상품",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            null,
            100,
            "thumbnail",
            ProductStatus.APPROVED
        ));
    }

    @Test
    void generatesAndStoresSummaryWhenMinimumReviewCountIsMet() throws Exception {
        saveReviews(10);
        when(generator.generate(any())).thenReturn(summaryResult("test-model-v1"));

        reviewSummaryService.refresh(product.getId());

        ReviewSummaryResponse response = reviewSummaryService.getSummary(product.getId());
        assertThat(response.available()).isTrue();
        assertThat(response.reviewCount()).isEqualTo(10);
        assertThat(response.pros()).containsExactly("키감이 부드럽습니다.");
        assertThat(response.cons()).containsExactly("무게가 무겁습니다.");
        assertThat(response.modelVersion()).isEqualTo("test-model-v1");
        assertThat(response.generatedAt()).isNotNull();
        assertThat(reviewSummaryRepository.findByProductId(product.getId())).isPresent();

        mockMvc.perform(get("/api/products/{productId}/review-summary", product.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(true))
            .andExpect(jsonPath("$.data.reviewCount").value(10))
            .andExpect(jsonPath("$.data.pros[0]").value("키감이 부드럽습니다."))
            .andExpect(jsonPath("$.data.generatedAt").value(endsWith("Z")));
    }

    @Test
    void keepsExistingSummaryWhenAiRequestTimesOut() {
        saveReviews(10);
        when(generator.generate(any())).thenReturn(summaryResult("test-model-v1"));
        reviewSummaryService.refresh(product.getId());
        String storedJson = reviewSummaryRepository.findByProductId(product.getId())
            .orElseThrow()
            .getSummaryJson();

        saveReview(11);
        when(generator.generate(any())).thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> reviewSummaryService.refresh(product.getId()))
            .isInstanceOf(ResourceAccessException.class);
        assertThat(reviewSummaryRepository.findByProductId(product.getId())
            .orElseThrow()
            .getSummaryJson()).isEqualTo(storedJson);
    }

    @Test
    void fallsBackToLocalLockWhenRedisIsUnavailable() {
        saveReviews(10);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenThrow(new RedisConnectionFailureException("redis unavailable"));
        when(generator.generate(any())).thenReturn(summaryResult("test-model-v1"));

        reviewSummaryService.refresh(product.getId());

        assertThat(reviewSummaryRepository.findByProductId(product.getId())).isPresent();
    }

    @Test
    void requestsRetryWithoutGeneratingWhenAnotherRequestOwnsTheLock() {
        saveReviews(10);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(false);

        assertThatThrownBy(() -> reviewSummaryService.refresh(product.getId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already running");

        verify(generator, never()).generate(any());
        assertThat(reviewSummaryRepository.findByProductId(product.getId())).isEmpty();
    }

    @Test
    void returnsUnavailableSummaryBelowMinimumReviewCount() {
        saveReviews(9);

        reviewSummaryService.refresh(product.getId());

        ReviewSummaryResponse response = reviewSummaryService.getSummary(product.getId());
        assertThat(response.available()).isFalse();
        assertThat(response.reviewCount()).isEqualTo(9);
        verify(generator, never()).generate(any());
    }

    private void saveReviews(int count) {
        for (int index = 1; index <= count; index++) {
            saveReview(index);
        }
    }

    private void saveReview(int index) {
        Order order = new Order(member, "summary-order-" + index);
        OrderItem orderItem = new OrderItem(
            product,
            product.getName(),
            product.getPrice(),
            1
        );
        order.addItem(orderItem);
        orderRepository.saveAndFlush(order);
        reviewRepository.saveAndFlush(new Review(
            member,
            product,
            orderItem,
            index % 2 == 0 ? 4 : 5,
            "리뷰 내용 " + index
        ));
    }

    private ReviewSummaryGenerator.Result summaryResult(String modelVersion) {
        return new ReviewSummaryGenerator.Result(
            List.of("키감이 부드럽습니다."),
            List.of("무게가 무겁습니다."),
            List.of("연결이 빠르다는 의견이 반복됩니다."),
            modelVersion
        );
    }
}
