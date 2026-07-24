package com.ymall.backend.payment.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.payment.entity.Payment;
import com.ymall.backend.payment.entity.PaymentResult;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = "order")
    Optional<Payment> findByOrderIdAndIdempotencyKey(Long orderId, String idempotencyKey);

    @EntityGraph(attributePaths = "order")
    Optional<Payment> findByPaymentKey(String paymentKey);

    @EntityGraph(attributePaths = "order")
    Optional<Payment> findFirstByOrderIdAndResultOrderByProcessedAtDesc(
        Long orderId,
        PaymentResult result
    );

    boolean existsByOrderIdAndResultAndPaymentKeyIsNotNull(
        Long orderId,
        PaymentResult result
    );

    @Query("""
        select distinct payment.order.id
        from Payment payment
        where payment.order.id in :orderIds
          and payment.result = :result
          and payment.paymentKey is not null
        """)
    Set<Long> findRefundSupportedOrderIds(
        @Param("orderIds") Collection<Long> orderIds,
        @Param("result") PaymentResult result
    );
}
