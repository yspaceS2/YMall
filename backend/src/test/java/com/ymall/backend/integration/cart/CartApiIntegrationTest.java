package com.ymall.backend.integration.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CartApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member member;
    private Product product;
    private String accessToken;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(new Member(
            "user@example.com",
            "password",
            "홍길동",
            MemberRole.ROLE_USER
        ));
        Category category = categoryRepository.save(new Category("전자기기", "electronics"));
        product = productRepository.save(product(category, "무선 키보드", ProductStatus.APPROVED, 10));
        accessToken = jwtTokenProvider.createAccessToken(member).accessToken();
    }

    @Test
    void addsDuplicateProductByIncreasingQuantityAndReturnsCart() throws Exception {
        addItem(product.getId(), 2)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.quantity").value(2));

        addItem(product.getId(), 3)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.quantity").value(5));

        mockMvc.perform(get("/api/cart")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].productId").value(product.getId()))
            .andExpect(jsonPath("$.data.items[0].quantity").value(5));

        assertThat(cartItemRepository.findAll()).hasSize(1);
    }

    @Test
    void updatesAndDeletesOwnedCartItem() throws Exception {
        addItem(product.getId(), 1).andExpect(status().isCreated());
        Long cartItemId = cartItemRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/api/cart/items/{cartItemId}", cartItemId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "quantity": 4
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.quantity").value(4));

        mockMvc.perform(delete("/api/cart/items/{cartItemId}", cartItemId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isNoContent());

        assertThat(cartItemRepository.findAll()).isEmpty();
    }

    @Test
    void rejectsUnavailableProductAndInsufficientStock() throws Exception {
        Category category = categoryRepository.findAll().get(0);
        Product draftProduct = productRepository.save(product(category, "임시 상품", ProductStatus.DRAFT, 10));

        addItem(draftProduct.getId(), 1)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_AVAILABLE"));

        addItem(product.getId(), 11)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void hidesCartItemFromAnotherMember() throws Exception {
        addItem(product.getId(), 1).andExpect(status().isCreated());
        Long cartItemId = cartItemRepository.findAll().get(0).getId();
        Member anotherMember = memberRepository.save(new Member(
            "another@example.com",
            "password",
            "김영희",
            MemberRole.ROLE_USER
        ));
        String anotherToken = jwtTokenProvider.createAccessToken(anotherMember).accessToken();

        mockMvc.perform(delete("/api/cart/items/{cartItemId}", cartItemId)
                .header(HttpHeaders.AUTHORIZATION, bearer(anotherToken)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("CART_ITEM_NOT_FOUND"));

        assertThat(cartItemRepository.findById(cartItemId)).isPresent();
    }

    @Test
    void rejectsInvalidQuantityAndMissingAuthentication() throws Exception {
        addItem(product.getId(), 0)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));

        mockMvc.perform(get("/api/cart"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    private org.springframework.test.web.servlet.ResultActions addItem(Long productId, int quantity)
        throws Exception {
        return mockMvc.perform(post("/api/cart/items")
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "productId": %d,
                    "quantity": %d
                }
                """.formatted(productId, quantity)));
    }

    private Product product(Category category, String name, ProductStatus status, int stock) {
        return new Product(
            category,
            name,
            "description",
            "YMall",
            BigDecimal.valueOf(39000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(4.5),
            stock,
            "thumbnail",
            status
        );
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
