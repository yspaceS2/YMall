package com.ymall.backend.settlement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.settlement.entity.SettlementRequestHistory;

public interface SettlementRequestHistoryRepository
    extends JpaRepository<SettlementRequestHistory, Long> {

    @EntityGraph(attributePaths = "actor")
    List<SettlementRequestHistory> findAllBySettlementRequestIdOrderByCreatedAtAsc(
        Long settlementRequestId
    );
}
