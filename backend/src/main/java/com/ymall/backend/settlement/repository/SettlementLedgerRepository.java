package com.ymall.backend.settlement.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.settlement.entity.SettlementEntryType;
import com.ymall.backend.settlement.entity.SettlementLedgerEntry;
import com.ymall.backend.settlement.entity.SettlementStatus;
import jakarta.persistence.LockModeType;

public interface SettlementLedgerRepository extends JpaRepository<SettlementLedgerEntry, Long> {

    boolean existsBySourceKey(String sourceKey);

    List<SettlementLedgerEntry> findAllByOrderIdAndStatus(
        Long orderId,
        SettlementStatus status
    );

    boolean existsByOrderItemIdAndEntryTypeAndStatus(
        Long orderItemId,
        SettlementEntryType entryType,
        SettlementStatus status
    );

    boolean existsByOrderItemIdAndEntryTypeAndStatusIn(
        Long orderItemId,
        SettlementEntryType entryType,
        List<SettlementStatus> statuses
    );

    @EntityGraph(attributePaths = {"order", "orderItem"})
    @Query("""
        select entry from SettlementLedgerEntry entry
        where entry.sellerProfile.id = :sellerProfileId
          and (:status is null or entry.status = :status)
          and (:from is null or entry.occurredAt >= :from)
          and (:to is null or entry.occurredAt < :to)
        order by entry.occurredAt desc, entry.id desc
        """)
    Page<SettlementLedgerEntry> findSellerLedger(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("status") SettlementStatus status,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select entry from SettlementLedgerEntry entry
        where entry.sellerProfile.id = :sellerProfileId
          and entry.status = com.ymall.backend.settlement.entity.SettlementStatus.AVAILABLE
        order by entry.id
        """)
    List<SettlementLedgerEntry> findAvailableForUpdate(
        @Param("sellerProfileId") Long sellerProfileId
    );

    @Query("""
        select entry from SettlementLedgerEntry entry
        where entry.sellerProfile.id = :sellerProfileId
          and entry.status = com.ymall.backend.settlement.entity.SettlementStatus.AVAILABLE
        order by entry.id
        """)
    List<SettlementLedgerEntry> findAvailable(
        @Param("sellerProfileId") Long sellerProfileId
    );

    List<SettlementLedgerEntry> findAllBySettlementRequestId(Long settlementRequestId);
}
