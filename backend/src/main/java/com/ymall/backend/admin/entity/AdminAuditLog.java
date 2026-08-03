package com.ymall.backend.admin.entity;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.member.entity.Member;

@Getter
@Entity
@Table(name = "admin_audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_member_id", nullable = false)
    private Member actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminGrade actorGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdminAuditTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminAuditAction action;

    @Column(length = 1000)
    private String beforeValue;

    @Column(length = 1000)
    private String afterValue;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AdminAuditLog(
        Member actor,
        AdminAuditTargetType targetType,
        Long targetId,
        AdminAuditAction action,
        String beforeValue,
        String afterValue,
        String reason
    ) {
        this.actor = actor;
        this.actorGrade = actor.getAdminGrade();
        this.targetType = targetType;
        this.targetId = targetId;
        this.action = action;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
