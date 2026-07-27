package com.ymall.backend.wishlist.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.common.PageResponse;
import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.wishlist.dto.WishlistProductResponse;
import com.ymall.backend.wishlist.dto.WishlistStatusResponse;
import com.ymall.backend.wishlist.entity.WishlistItem;
import com.ymall.backend.wishlist.repository.WishlistItemRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private static final int MAX_PAGE_SIZE = 100;

    private final WishlistItemRepository wishlistItemRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public PageResponse<WishlistProductResponse> getWishlist(
        Long memberId,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return PageResponse.from(
            wishlistItemRepository.findVisibleByMemberId(
                memberId,
                ProductStatus.DELETED,
                pageable
            ).map(WishlistProductResponse::from)
        );
    }

    public WishlistStatusResponse getStatus(Long memberId, Long productId) {
        return new WishlistStatusResponse(
            productId,
            wishlistItemRepository.existsByMemberIdAndProductId(memberId, productId)
        );
    }

    @Transactional
    public WishlistStatusResponse add(Long memberId, Long productId) {
        Member member = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStatus() != ProductStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }
        if (!wishlistItemRepository.existsByMemberIdAndProductId(memberId, productId)) {
            wishlistItemRepository.save(new WishlistItem(member, product));
        }
        return new WishlistStatusResponse(productId, true);
    }

    @Transactional
    public void remove(Long memberId, Long productId) {
        wishlistItemRepository.deleteByMemberIdAndProductId(memberId, productId);
    }
}
