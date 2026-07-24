package com.ymall.backend.payment.refund.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.ymall.backend.payment.refund.entity.PaymentRefund;
import com.ymall.backend.payment.refund.entity.PaymentRefundStatus;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {

    @EntityGraph(attributePaths = {"items", "items.orderItem"})
    Optional<PaymentRefund> findByOrderIdAndIdempotencyKey(
        Long orderId,
        String idempotencyKey
    );

    @EntityGraph(attributePaths = {"items", "items.orderItem"})
    List<PaymentRefund> findAllByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "items.orderItem", "order", "payment"})
    @Query("select refund from PaymentRefund refund where refund.id = :refundId")
    Optional<PaymentRefund> findByIdForUpdate(@Param("refundId") Long refundId);

    boolean existsByOrderIdAndStatusIn(
        Long orderId,
        List<PaymentRefundStatus> statuses
    );
}
