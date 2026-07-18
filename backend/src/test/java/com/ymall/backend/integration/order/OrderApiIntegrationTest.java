package com.ymall.backend.integration.order;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.ymall.backend.member.entity.MemberAddress;
import com.ymall.backend.member.repository.MemberAddressRepository;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.order.entity.Order;
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderStatus;
import com.ymall.backend.order.repository.OrderRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.CategoryRepository;
import com.ymall.backend.product.repository.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderApiIntegrationTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private MemberAddressRepository memberAddressRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member member;
    private MemberAddress deliveryAddress;
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
        deliveryAddress = memberAddressRepository.save(new MemberAddress(
            member, "Home", "Recipient", "01012345678", "12159", "186 Biryong-ro", "101", true
        ));
        Category category = categoryRepository.save(new Category("전자기기", "electronics"));
        product = productRepository.save(new Product(
            category,
            "무선 키보드",
            "description",
            "YMall",
            BigDecimal.valueOf(10000),
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(4.5),
            10,
            "thumbnail",
            ProductStatus.APPROVED
        ));
        accessToken = jwtTokenProvider.createAccessToken(member).accessToken();
    }

    @Test
    void createsOrderWithProductSnapshotAndClearsCart() throws Exception {
        cartItemRepository.save(new CartItem(member, product, 2));

        createOrder("request-1")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.data.totalAmount").value(18000))
            .andExpect(jsonPath("$.data.items[0].productName").value("무선 키보드"))
            .andExpect(jsonPath("$.data.items[0].unitPrice").value(9000))
            .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        Order order = orderRepository.findAll().get(0);
        OrderItem orderItem = order.getItems().get(0);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(orderItem.getProductName()).isEqualTo("무선 키보드");
        assertThat(orderItem.getUnitPrice()).isEqualByComparingTo("9000.00");
        assertThat(product.getStock()).isEqualTo(8);
        assertThat(cartItemRepository.findAll()).isEmpty();
    }

    @Test
    void keepsDeliveryAddressSnapshotAfterMemberAddressChanges() throws Exception {
        MemberAddress address = memberAddressRepository.save(new MemberAddress(member, "집", "수령인",
            "01012345678", "12159", "비룡로 186", "101동", true));
        cartItemRepository.save(new CartItem(member, product, 1));

        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"idempotencyKey":"address-snapshot","addressId":%d}
                    """.formatted(address.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.deliveryAddress.roadAddress").value("비룡로 186"))
            .andExpect(jsonPath("$.data.deliveryAddress.detailAddress").value("101동"));

        address.update("집", "수령인", "01012345678", "12159", "변경된 주소", "202호");
        memberAddressRepository.flush();

        Order order = orderRepository.findAll().get(0);
        assertThat(order.getDeliveryAddress().getRoadAddress()).isEqualTo("비룡로 186");
        assertThat(order.getDeliveryAddress().getDetailAddress()).isEqualTo("101동");
    }

    @Test
    void returnsSameOrderWithoutDecreasingStockForDuplicateRequest() throws Exception {
        cartItemRepository.save(new CartItem(member, product, 2));
        createOrder("request-1").andExpect(status().isCreated());
        Long orderId = orderRepository.findAll().get(0).getId();

        createOrder("request-1")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.orderId").value(orderId));

        assertThat(orderRepository.findAll()).hasSize(1);
        assertThat(product.getStock()).isEqualTo(8);
    }

    @Test
    void storesOrderAmountForMaximumProductPriceAndQuantity() throws Exception {
        BigDecimal maximumPrice = new BigDecimal("9999999999.99");
        product.update(
            product.getCategory(),
            product.getName(),
            product.getDescription(),
            product.getBrand(),
            maximumPrice,
            BigDecimal.ZERO,
            Integer.MAX_VALUE,
            product.getThumbnailUrl()
        );
        cartItemRepository.save(new CartItem(member, product, Integer.MAX_VALUE));

        createOrder("large-amount-request").andExpect(status().isCreated());

        Order order = orderRepository.findAll().get(0);
        BigDecimal expectedAmount = maximumPrice.multiply(BigDecimal.valueOf(Integer.MAX_VALUE));
        assertThat(order.getTotalAmount()).isEqualByComparingTo(expectedAmount);
        assertThat(order.getItems().get(0).getLineTotal()).isEqualByComparingTo(expectedAmount);
    }

    @Test
    void rejectsInsufficientStockWithoutCreatingOrder() throws Exception {
        product.update(
            product.getCategory(),
            product.getName(),
            product.getDescription(),
            product.getBrand(),
            product.getPrice(),
            product.getDiscountPercentage(),
            1,
            product.getThumbnailUrl()
        );
        cartItemRepository.save(new CartItem(member, product, 2));

        createOrder("request-1")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_STOCK"));

        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(product.getStock()).isEqualTo(1);
        assertThat(cartItemRepository.findAll()).hasSize(1);
    }

    @Test
    void rejectsEmptyCartAndMissingAuthentication() throws Exception {
        createOrder("request-1")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("CART_EMPTY"));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson("request-2")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void rejectsOrderWithoutDeliveryAddress() throws Exception {
        cartItemRepository.save(new CartItem(member, product, 1));

        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotencyKey\":\"missing-address\"}"))
            .andExpect(status().isBadRequest());

        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    void rejectsDeliveryAddressOwnedByAnotherMember() throws Exception {
        Member otherMember = memberRepository.save(new Member(
            "other@example.com", "password", "Other", MemberRole.ROLE_USER
        ));
        MemberAddress otherAddress = memberAddressRepository.save(new MemberAddress(
            otherMember, "Home", "Other", "01099999999", "12345", "Other road", "202", true
        ));
        cartItemRepository.save(new CartItem(member, product, 1));

        mockMvc.perform(post("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"idempotencyKey":"foreign-address","addressId":%d}
                    """.formatted(otherAddress.getId())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("MEMBER_ADDRESS_NOT_FOUND"));

        assertThat(orderRepository.findAll()).isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions createOrder(String idempotencyKey)
        throws Exception {
        return mockMvc.perform(post("/api/orders")
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(orderJson(idempotencyKey)));
    }

    private String orderJson(String idempotencyKey) {
        return """
            {
                "idempotencyKey": "%s",
                "addressId": %d
            }
            """.formatted(idempotencyKey, deliveryAddress.getId());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
