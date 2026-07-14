package com.ymall.backend.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import com.ymall.backend.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = "product")
    List<CartItem> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CartItem> findByMemberIdAndProductId(Long memberId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CartItem> findByIdAndMemberId(Long cartItemId, Long memberId);
}
