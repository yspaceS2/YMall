package com.ymall.backend.settlement.repository;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ymall.backend.settlement.entity.SettlementRequest;
import com.ymall.backend.settlement.entity.SettlementRequestStatus;

public interface SettlementRequestRepository extends JpaRepository<SettlementRequest, Long> {

    Optional<SettlementRequest> findBySellerProfileIdAndPeriodStart(
        Long sellerProfileId,
        LocalDate periodStart
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select request from SettlementRequest request
        where request.sellerProfile.id = :sellerProfileId
          and request.periodStart = :periodStart
        """)
    Optional<SettlementRequest> findBySellerAndPeriodForUpdate(
        @Param("sellerProfileId") Long sellerProfileId,
        @Param("periodStart") LocalDate periodStart
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from SettlementRequest request where request.id = :requestId")
    Optional<SettlementRequest> findByIdForUpdate(@Param("requestId") Long requestId);

    @EntityGraph(attributePaths = "sellerProfile")
    Page<SettlementRequest> findAllBySellerProfileIdOrderByPeriodStartDesc(
        Long sellerProfileId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = "sellerProfile")
    @Query("""
        select request from SettlementRequest request
        where (:status is null or request.status = :status)
        order by request.createdAt desc, request.id desc
        """)
    Page<SettlementRequest> findAdminRequests(
        @Param("status") SettlementRequestStatus status,
        Pageable pageable
    );
}
