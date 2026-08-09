package com.ymall.backend.support.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.member.entity.Member;

@Getter
@Entity
@Table(name = "support_inquiries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportRequesterType requesterType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupportInquiryCategory category;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupportInquiryStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private Member assignedAdmin;

    private Long relatedOrderId;
    private Long relatedProductId;
    private Long relatedSettlementId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime closedAt;

    public SupportInquiry(
        Member member,
        SupportRequesterType requesterType,
        SupportInquiryCategory category,
        String title,
        Long relatedOrderId,
        Long relatedProductId,
        Long relatedSettlementId
    ) {
        this.member = member;
        this.requesterType = requesterType;
        this.category = category;
        this.title = title;
        this.status = SupportInquiryStatus.WAITING;
        this.relatedOrderId = relatedOrderId;
        this.relatedProductId = relatedProductId;
        this.relatedSettlementId = relatedSettlementId;
    }

    public void assign(Member admin) {
        this.assignedAdmin = admin;
        if (status == SupportInquiryStatus.WAITING || status == SupportInquiryStatus.ANSWERED) {
            status = SupportInquiryStatus.IN_PROGRESS;
        }
    }

    public void markWaiting() {
        status = SupportInquiryStatus.WAITING;
    }

    public void markAnswered() {
        status = SupportInquiryStatus.ANSWERED;
    }

    public void markLiveRequested() {
        status = SupportInquiryStatus.LIVE_REQUESTED;
    }

    public void markLiveOffered() {
        status = SupportInquiryStatus.LIVE_OFFERED;
    }

    public void markLiveActive() {
        status = SupportInquiryStatus.LIVE_ACTIVE;
    }

    public void resumeGeneralInquiry() {
        status = assignedAdmin == null
            ? SupportInquiryStatus.WAITING
            : SupportInquiryStatus.IN_PROGRESS;
    }

    public void close() {
        status = SupportInquiryStatus.CLOSED;
        closedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
