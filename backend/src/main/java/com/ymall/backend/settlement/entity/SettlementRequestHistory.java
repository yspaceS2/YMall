package com.ymall.backend.settlement.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.member.entity.Member;

@Getter
@Entity
@Table(name = "settlement_request_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementRequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_request_id", nullable = false, updatable = false)
    private SettlementRequest settlementRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20, updatable = false)
    private SettlementRequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20, updatable = false)
    private SettlementRequestStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_member_id", nullable = false, updatable = false)
    private Member actor;

    @Column(length = 500, updatable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SettlementRequestHistory(
        SettlementRequest settlementRequest,
        SettlementRequestStatus fromStatus,
        SettlementRequestStatus toStatus,
        Member actor,
        String reason
    ) {
        this.settlementRequest = settlementRequest;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
