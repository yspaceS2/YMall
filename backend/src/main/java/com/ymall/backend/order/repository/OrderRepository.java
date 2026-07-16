package com.ymall.backend.order.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.ymall.backend.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByMemberIdAndIdempotencyKey(Long memberId, String idempotencyKey);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndMemberId(Long orderId, Long memberId);

    Page<Order> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select orders from Order orders where orders.id = :orderId and orders.member.id = :memberId")
    Optional<Order> findByIdAndMemberIdForUpdate(
        @Param("orderId") Long orderId,
        @Param("memberId") Long memberId
    );
}
