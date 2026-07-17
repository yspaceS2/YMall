package com.ymall.backend.integration.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.entity.OrderStatus;
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
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EntityManager entityManager;

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

    private Member saveMember(String email, MemberRole role) {
        return memberRepository.save(new Member(email, "password", email, role));
    }

    private Product saveProduct(SellerProfile profile, String name) {
        Product product = new Product(
            category,
            name,
            "상품 설명",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            10,
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
