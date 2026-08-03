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
@Table(name = "support_chat_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false, unique = true)
    private SupportInquiry inquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Member admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportChatInitiator initiatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportChatStatus status;

    private LocalDateTime requestedAt;
    private LocalDateTime offeredAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public SupportChatSession(
        SupportInquiry inquiry,
        Member admin,
        SupportChatInitiator initiatedBy,
        LocalDateTime expiresAt
    ) {
        this.inquiry = inquiry;
        this.admin = admin;
        this.initiatedBy = initiatedBy;
        this.status = SupportChatStatus.WAITING;
        this.expiresAt = expiresAt;
        if (initiatedBy == SupportChatInitiator.USER_REQUEST) {
            requestedAt = LocalDateTime.now();
        } else {
            offeredAt = LocalDateTime.now();
        }
    }

    public void accept(Member assignedAdmin) {
        this.admin = assignedAdmin;
        this.status = SupportChatStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
    }

    public void reject() {
        status = SupportChatStatus.REJECTED;
        endedAt = LocalDateTime.now();
    }

    public void end() {
        status = SupportChatStatus.ENDED;
        endedAt = LocalDateTime.now();
    }

    public void expire() {
        status = SupportChatStatus.EXPIRED;
        endedAt = LocalDateTime.now();
    }

    public void renew(Member assignedAdmin, SupportChatInitiator initiator, LocalDateTime expiry) {
        admin = assignedAdmin;
        initiatedBy = initiator;
        status = SupportChatStatus.WAITING;
        requestedAt = initiator == SupportChatInitiator.USER_REQUEST ? LocalDateTime.now() : null;
        offeredAt = initiator == SupportChatInitiator.ADMIN_OFFER ? LocalDateTime.now() : null;
        startedAt = null;
        endedAt = null;
        expiresAt = expiry;
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
