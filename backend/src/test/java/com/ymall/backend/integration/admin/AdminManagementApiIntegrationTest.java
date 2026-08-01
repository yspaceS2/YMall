package com.ymall.backend.integration.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.ymall.backend.seller.repository.SellerProfileRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminManagementApiIntegrationTest {

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

    private Member admin;
    private Member seller;
    private Member buyer;
    private SellerProfile sellerProfile;
    private Category category;
    private String adminToken;
    private String sellerToken;

    @BeforeEach
    void setUp() {
        admin = saveMember("admin@example.com", "관리자", MemberRole.ROLE_ADMIN);
        seller = saveMember("seller@example.com", "판매자", MemberRole.ROLE_SELLER);
        buyer = saveMember("buyer@example.com", "구매자", MemberRole.ROLE_USER);
        sellerProfile = sellerProfileRepository.save(new SellerProfile(
            seller,
            "테스트 상점",
            "123-45-67890",
            "관리자 테스트 판매자"
        ));
        category = categoryRepository.save(new Category("전자기기", "electronics"));
        adminToken = token(admin);
        sellerToken = token(seller);
    }

    @Test
    void adminApprovesPendingProductAndMakesItPublic() throws Exception {
        Product product = saveProduct("승인 대기 상품", ProductStatus.PENDING);

        mockMvc.perform(get("/api/admin/products")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].productId").value(product.getId()))
            .andExpect(jsonPath("$.data.content[0].storeName").value("테스트 상점"))
            .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));

        mockMvc.perform(patch("/api/admin/products/{productId}/status", product.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/products/{productId}", product.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.productId").value(product.getId()));
    }

    @Test
    void rejectedProductRemainsHiddenFromPublicApi() throws Exception {
        Product product = saveProduct("반려 대상 상품", ProductStatus.PENDING);

        mockMvc.perform(patch("/api/admin/products/{productId}/status", product.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status":"REJECTED",
                      "rejectionReason":"상품 설명을 보완해 주세요."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("REJECTED"))
            .andExpect(jsonPath("$.data.rejectionReason")
                .value("상품 설명을 보완해 주세요."));

        mockMvc.perform(get("/api/seller/products")
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].status").value("REJECTED"))
            .andExpect(jsonPath("$.data.content[0].rejectionReason")
                .value("상품 설명을 보완해 주세요."));

        mockMvc.perform(get("/api/products/{productId}", product.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void nonAdminCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                .header(HttpHeaders.AUTHORIZATION, bearer(sellerToken)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    void rejectionRequiresReasonAndAdminCanReadReviewDetails() throws Exception {
        Product product = saveProduct("검수 상세 상품", ProductStatus.PENDING);

        mockMvc.perform(get("/api/admin/products/{productId}", product.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.productId").value(product.getId()))
            .andExpect(jsonPath("$.data.description").value("상품 설명"))
            .andExpect(jsonPath("$.data.price").value(10000))
            .andExpect(jsonPath("$.data.images").isArray())
            .andExpect(jsonPath("$.data.detailImages").isArray());

        mockMvc.perform(patch("/api/admin/products/{productId}/status", product.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJECTED\",\"rejectionReason\":\"  \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void adminFiltersProductsByStatusAndKeyword() throws Exception {
        saveProduct("검색 대상 운동화", ProductStatus.APPROVED);
        saveProduct("검색 제외 상품", ProductStatus.PENDING);

        mockMvc.perform(get("/api/admin/products")
                .param("status", "APPROVED")
                .param("keyword", "검색대상운동화")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].name").value("검색 대상 운동화"))
            .andExpect(jsonPath("$.data.content[0].images").isEmpty())
            .andExpect(jsonPath("$.data.content[0].detailImages").isEmpty());
    }

    @Test
    void adminReadsPendingProductsAfterFirstPage() throws Exception {
        for (int index = 1; index <= 21; index++) {
            saveProduct("승인 대기 상품 " + index, ProductStatus.PENDING);
        }

        mockMvc.perform(get("/api/admin/products")
                .param("page", "2")
                .param("size", "20")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.page").value(2))
            .andExpect(jsonPath("$.data.totalElements").value(21))
            .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void adminReadsMembersSellersAndOrders() throws Exception {
        Product product = saveProduct("주문 상품", ProductStatus.APPROVED);
        Order order = new Order(buyer, "admin-management-test");
        order.addItem(new OrderItem(product, product.getName(), product.getPrice(), 2));
        order.completePayment();
        orderRepository.save(order);

        mockMvc.perform(get("/api/admin/members")
                .param("keyword", "buyer@example.com")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].memberId").value(buyer.getId()));

        mockMvc.perform(get("/api/admin/members/{memberId}", buyer.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("buyer@example.com"));

        mockMvc.perform(get("/api/admin/sellers")
                .param("keyword", "테스트 상점")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].storeName").value("테스트 상점"))
            .andExpect(jsonPath("$.data.content[0].email").value("seller@example.com"));

        mockMvc.perform(get(
                "/api/admin/sellers/{sellerId}",
                sellerProfile.getId()
            ).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.storeName").value("테스트 상점"));

        mockMvc.perform(get("/api/admin/orders")
                .param("keyword", order.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content[0].orderId").value(order.getId()))
            .andExpect(jsonPath("$.data.content[0].memberEmail").value("buyer@example.com"))
            .andExpect(jsonPath("$.data.content[0].items[0].productName").value("주문 상품"));

        mockMvc.perform(get("/api/admin/orders/{orderId}", order.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderId").value(order.getId()))
            .andExpect(jsonPath("$.data.items[0].productName").value("주문 상품"));
    }

    @Test
    void adminCannotApplyUnsupportedStatusOrReviewFinishedProductAgain() throws Exception {
        Product product = saveProduct("상태 검증 상품", ProductStatus.PENDING);

        mockMvc.perform(patch("/api/admin/products/{productId}/status", product.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DRAFT\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        assertThat(product.getStatus()).isEqualTo(ProductStatus.PENDING);

        mockMvc.perform(patch("/api/admin/products/{productId}/status", product.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/products/{productId}/status", product.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJECTED\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("PRODUCT_REVIEW_NOT_ALLOWED"));
    }

    @Test
    void adminCreatesSearchesAndUpdatesCategory() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "스마트폰",
                        "slug": "smartphones",
                        "parentId": %d,
                        "displayOrder": 1,
                        "active": true
                    }
                    """.formatted(category.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.parentId").value(category.getId()))
            .andExpect(jsonPath("$.data.depth").value(2));

        mockMvc.perform(get("/api/admin/categories")
                .param("keyword", "smart")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].slug").value("smartphones"));

        mockMvc.perform(put("/api/admin/categories/{categoryId}", category.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "디지털",
                        "slug": "digital",
                        "parentId": null,
                        "displayOrder": 2,
                        "active": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("디지털"))
            .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void adminCannotCreateCategoryBeyondThirdDepth() throws Exception {
        Category secondDepth = categoryRepository.save(new Category(
            "모바일",
            "mobile",
            category,
            2,
            0,
            true
        ));
        Category thirdDepth = categoryRepository.save(new Category(
            "스마트폰",
            "smartphones",
            secondDepth,
            3,
            0,
            true
        ));

        mockMvc.perform(post("/api/admin/categories")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "안드로이드",
                        "slug": "android",
                        "parentId": %d,
                        "displayOrder": 1,
                        "active": true
                    }
                    """.formatted(thirdDepth.getId())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("CATEGORY_DEPTH_EXCEEDED"));
    }

    @Test
    void adminCannotDeleteCategoryWithConnectedProduct() throws Exception {
        saveProduct("연결 상품", ProductStatus.PENDING);

        mockMvc.perform(delete("/api/admin/categories/{categoryId}", category.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("CATEGORY_DELETE_NOT_ALLOWED"));
    }

    private Member saveMember(String email, String name, MemberRole role) {
        return memberRepository.save(new Member(email, "password", name, role));
    }

    private Product saveProduct(String name, ProductStatus status) {
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
            status
        );
        product.assignSellerProfile(sellerProfile);
        return productRepository.save(product);
    }

    private String token(Member member) {
        return jwtTokenProvider.createAccessToken(member).accessToken();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
