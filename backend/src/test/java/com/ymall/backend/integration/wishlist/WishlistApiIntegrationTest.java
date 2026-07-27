package com.ymall.backend.integration.wishlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.ymall.backend.global.security.JwtTokenProvider;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.wishlist.repository.WishlistItemRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WishlistApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member member;
    private Category category;
    private Product product;
    private String accessToken;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(new Member(
            "wishlist-user@example.com",
            "password",
            "찜 테스트 회원",
            MemberRole.ROLE_USER
        ));
        category = categoryRepository.save(new Category("찜 테스트", "wishlist-test"));
        product = productRepository.save(product("승인 상품", ProductStatus.APPROVED, 10));
        accessToken = jwtTokenProvider.createAccessToken(member).accessToken();
    }

    @Test
    void repeatedAddCreatesOneItemAndStatusPersists() throws Exception {
        add(product.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.productId").value(product.getId()))
            .andExpect(jsonPath("$.data.wished").value(true));

        add(product.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.wished").value(true));

        mockMvc.perform(get("/api/members/me/wishlist/products/{productId}", product.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.wished").value(true));

        assertThat(wishlistItemRepository.findAll()).hasSize(1);
    }

    @Test
    void listShowsUnavailableStateAndExcludesDeletedProduct() throws Exception {
        Product soldOutProduct = productRepository.save(
            product("품절 상품", ProductStatus.APPROVED, 0)
        );
        Product deletedProduct = productRepository.save(
            product("삭제 상품", ProductStatus.APPROVED, 5)
        );
        add(product.getId()).andExpect(status().isOk());
        add(soldOutProduct.getId()).andExpect(status().isOk());
        add(deletedProduct.getId()).andExpect(status().isOk());

        product.requestApproval();
        deletedProduct.delete();
        productRepository.flush();

        mockMvc.perform(get("/api/members/me/wishlist")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(2))
            .andExpect(jsonPath("$.data.content[*].status").value(hasItem("PENDING")))
            .andExpect(jsonPath("$.data.content[*].stock").value(hasItem(0)));
    }

    @Test
    void removeIsIdempotentAndWishlistIsOwnedByMember() throws Exception {
        add(product.getId()).andExpect(status().isOk());
        Member anotherMember = memberRepository.save(new Member(
            "wishlist-another@example.com",
            "password",
            "다른 회원",
            MemberRole.ROLE_USER
        ));
        String anotherToken = jwtTokenProvider.createAccessToken(anotherMember).accessToken();

        mockMvc.perform(get("/api/members/me/wishlist/products/{productId}", product.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(anotherToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.wished").value(false));

        remove(product.getId()).andExpect(status().isNoContent());
        remove(product.getId()).andExpect(status().isNoContent());

        assertThat(wishlistItemRepository.findAll()).isEmpty();
    }

    @Test
    void rejectsUnavailableProductAndMissingAuthentication() throws Exception {
        Product draftProduct = productRepository.save(
            product("임시 상품", ProductStatus.DRAFT, 10)
        );

        add(draftProduct.getId())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_AVAILABLE"));

        mockMvc.perform(get("/api/members/me/wishlist"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    private org.springframework.test.web.servlet.ResultActions add(Long productId)
        throws Exception {
        return mockMvc.perform(post(
            "/api/members/me/wishlist/products/{productId}",
            productId
        ).header(HttpHeaders.AUTHORIZATION, bearer(accessToken)));
    }

    private org.springframework.test.web.servlet.ResultActions remove(Long productId)
        throws Exception {
        return mockMvc.perform(delete(
            "/api/members/me/wishlist/products/{productId}",
            productId
        ).header(HttpHeaders.AUTHORIZATION, bearer(accessToken)));
    }

    private Product product(String name, ProductStatus status, int stock) {
        return new Product(
            category,
            name,
            "찜 통합 테스트 상품",
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
