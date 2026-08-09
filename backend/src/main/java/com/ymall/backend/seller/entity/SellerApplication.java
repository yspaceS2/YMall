package com.ymall.backend.seller.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.member.entity.Member;

@Getter
@Entity
@Table(name = "seller_applications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false, length = 100)
    private String storeName;

    @Column(nullable = false, length = 20)
    private String businessNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerApplicationStatus status;

    @Column(length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Member reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public SellerApplication(
        Member member,
        String storeName,
        String businessNumber,
        String description
    ) {
        this.member = member;
        this.storeName = storeName;
        this.businessNumber = businessNumber;
        this.description = description;
        this.status = SellerApplicationStatus.PENDING;
    }

    public void resubmit(String storeName, String businessNumber, String description) {
        this.storeName = storeName;
        this.businessNumber = businessNumber;
        this.description = description;
        this.status = SellerApplicationStatus.PENDING;
        this.rejectionReason = null;
        this.reviewedBy = null;
        this.reviewedAt = null;
    }

    public void approve(Member reviewer) {
        this.status = SellerApplicationStatus.APPROVED;
        this.rejectionReason = null;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(Member reviewer, String reason) {
        this.status = SellerApplicationStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    public void requestRevision(Member reviewer, String reason) {
        this.status = SellerApplicationStatus.NEEDS_REVISION;
        this.rejectionReason = reason;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
