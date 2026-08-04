package com.ymall.backend.payment.repository;

import java.util.Collection;
import java.util.List;
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

    @Query("""
        select payment.order.member.id, coalesce(sum(payment.approvedAmount), 0)
        from Payment payment
        where payment.order.member.id in :memberIds
          and payment.result = :result
        group by payment.order.member.id
        """)
    List<Object[]> sumApprovedAmountByMemberIds(
        @Param("memberIds") Collection<Long> memberIds,
        @Param("result") PaymentResult result
    );
}
