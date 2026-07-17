package com.ymall.backend.cart.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.cart.dto.CartItemAddRequest;
import com.ymall.backend.cart.dto.CartItemQuantityUpdateRequest;
import com.ymall.backend.cart.dto.CartItemResponse;
import com.ymall.backend.cart.dto.CartResponse;
import com.ymall.backend.cart.entity.CartItem;
import com.ymall.backend.cart.mapper.CartMapper;
import com.ymall.backend.cart.repository.CartItemRepository;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    public CartResponse getCart(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        List<CartItemResponse> items = cartItemRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
            .stream()
            .map(cartMapper::toCartItemResponse)
            .toList();
        return new CartResponse(items);
    }

    @Transactional
    public CartItemResponse addItem(Long memberId, CartItemAddRequest request) {
        Member member = findMemberForUpdate(memberId);
        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        validateAvailable(product);

        CartItem cartItem = cartItemRepository.findByMemberIdAndProductId(memberId, product.getId())
            .orElseGet(() -> new CartItem(member, product, request.quantity()));
        int targetQuantity = calculateTargetQuantity(cartItem, request.quantity());
        validateStock(product, targetQuantity);
        cartItem.changeQuantity(targetQuantity);

        return cartMapper.toCartItemResponse(cartItemRepository.save(cartItem));
    }

    @Transactional
    public CartItemResponse updateQuantity(
        Long memberId,
        Long cartItemId,
        CartItemQuantityUpdateRequest request
    ) {
        CartItem cartItem = findOwnedItemForUpdate(memberId, cartItemId);
        validateAvailable(cartItem.getProduct());
        validateStock(cartItem.getProduct(), request.quantity());
        cartItem.changeQuantity(request.quantity());

        return cartMapper.toCartItemResponse(cartItem);
    }

    @Transactional
    public void deleteItem(Long memberId, Long cartItemId) {
        CartItem cartItem = findOwnedItemForUpdate(memberId, cartItemId);
        cartItemRepository.delete(cartItem);
    }

    private Member findMemberForUpdate(Long memberId) {
        return memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private CartItem findOwnedItemForUpdate(Long memberId, Long cartItemId) {
        return cartItemRepository.findByIdAndMemberId(cartItemId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    private int calculateTargetQuantity(CartItem cartItem, int addedQuantity) {
        if (cartItem.getId() == null) {
            return addedQuantity;
        }

        long targetQuantity = (long) cartItem.getQuantity() + addedQuantity;
        if (targetQuantity > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        return (int) targetQuantity;
    }

    private void validateAvailable(Product product) {
        if (product.getStatus() != ProductStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }
    }

    private void validateStock(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }
}
