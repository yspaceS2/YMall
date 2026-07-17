package com.ymall.backend.review.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
import com.ymall.backend.order.entity.OrderItem;
import com.ymall.backend.order.entity.OrderItemFulfillmentStatus;
import com.ymall.backend.order.repository.OrderItemRepository;
import com.ymall.backend.product.entity.Product;
import com.ymall.backend.product.entity.ProductStatus;
import com.ymall.backend.product.repository.ProductRepository;
import com.ymall.backend.review.dto.ReviewCreateRequest;
import com.ymall.backend.review.dto.ReviewResponse;
import com.ymall.backend.review.dto.ReviewUpdateRequest;
import com.ymall.backend.review.entity.Review;
import com.ymall.backend.review.repository.ReviewRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public PageResponse<ReviewResponse> getProductReviews(Long productId, int page, int size) {
        if (!productRepository.existsByIdAndStatus(productId, ProductStatus.APPROVED)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return PageResponse.from(
            reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable(page, size))
                .map(ReviewResponse::from)
        );
    }

    public PageResponse<ReviewResponse> getMyReviews(Long memberId, int page, int size) {
        return PageResponse.from(
            reviewRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable(page, size))
                .map(ReviewResponse::from)
        );
    }

    @Transactional
    public ReviewResponse createReview(Long memberId, ReviewCreateRequest request) {
        OrderItem orderItem = orderItemRepository
            .findByIdAndOrderMemberIdAndFulfillmentStatus(
                request.orderItemId(),
                memberId,
                OrderItemFulfillmentStatus.DELIVERED
            )
            .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED));
        Product product = lockProduct(orderItem.getProduct().getId());
        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Review review = reviewRepository.saveAndFlush(new Review(
            member,
            product,
            orderItem,
            request.rating(),
            request.content().trim()
        ));
        refreshProductRating(product);
        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse updateReview(Long memberId, Long reviewId, ReviewUpdateRequest request) {
        Review review = getOwnedReview(memberId, reviewId);
        Product product = lockProduct(review.getProduct().getId());
        review.update(request.rating(), request.content().trim());
        reviewRepository.flush();
        refreshProductRating(product);
        return ReviewResponse.from(review);
    }

    @Transactional
    public void deleteReview(Long memberId, Long reviewId) {
        Review review = getOwnedReview(memberId, reviewId);
        Product product = lockProduct(review.getProduct().getId());
        reviewRepository.delete(review);
        reviewRepository.flush();
        refreshProductRating(product);
    }

    private Review getOwnedReview(Long memberId, Long reviewId) {
        return reviewRepository.findByIdAndMemberId(reviewId, memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private Product lockProduct(Long productId) {
        return productRepository.findByIdForRatingUpdate(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void refreshProductRating(Product product) {
        Double average = reviewRepository.findAverageRatingByProductId(product.getId());
        product.updateRating(average == null
            ? null
            : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(
            Math.max(page - 1, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }
}
