package com.ymall.backend.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ymall.backend.cart.dto.CartItemAddRequest;
import com.ymall.backend.cart.dto.CartItemResponse;
import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.mapper.CartMapper;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.MemberRole;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.product.entity.Category;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartService cartService;

    @Test
    void addsQuantitiesWhenProductAlreadyExistsInCart() {
        Member member = member();
        Product product = product(ProductStatus.APPROVED, 10);
        CartItem cartItem = new CartItem(member, product, 2);
        ReflectionTestUtils.setField(cartItem, "id", 1L);
        CartItemResponse response = response(5);

        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(cartItemRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.of(cartItem));
        given(cartItemRepository.save(cartItem)).willReturn(cartItem);
        given(cartMapper.toCartItemResponse(cartItem)).willReturn(response);

        CartItemResponse result = cartService.addItem(1L, new CartItemAddRequest(1L, 3));

        assertThat(cartItem.getQuantity()).isEqualTo(5);
        assertThat(result.quantity()).isEqualTo(5);
        then(cartItemRepository).should().save(cartItem);
    }

    @Test
    void rejectsProductThatIsNotApproved() {
        Member member = member();
        Product product = product(ProductStatus.DRAFT, 10);
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(1L, new CartItemAddRequest(1L, 1)))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.PRODUCT_NOT_AVAILABLE);
    }

    @Test
    void rejectsQuantityGreaterThanStock() {
        Member member = member();
        Product product = product(ProductStatus.APPROVED, 2);
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(cartItemRepository.findByMemberIdAndProductId(1L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(1L, new CartItemAddRequest(1L, 3)))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    void rejectsCartItemOwnedByAnotherMember() {
        given(cartItemRepository.findByIdAndMemberId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.deleteItem(1L, 10L))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND);

        then(cartItemRepository).shouldHaveNoMoreInteractions();
    }

    private Member member() {
        Member member = new Member("user@example.com", "password", "홍길동", MemberRole.ROLE_USER);
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private Product product(ProductStatus status, int stock) {
        Product product = new Product(
            new Category("전자기기", "electronics"),
            "무선 키보드",
            "description",
            "YMall",
            BigDecimal.valueOf(39000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(4.5),
            stock,
            "thumbnail",
            status
        );
        ReflectionTestUtils.setField(product, "id", 1L);
        return product;
    }

    private CartItemResponse response(int quantity) {
        return new CartItemResponse(
            1L,
            1L,
            "무선 키보드",
            "thumbnail",
            BigDecimal.valueOf(39000),
            BigDecimal.ZERO,
            10,
            ProductStatus.APPROVED,
            quantity
        );
    }
}
