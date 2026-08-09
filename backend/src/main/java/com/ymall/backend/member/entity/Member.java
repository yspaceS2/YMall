package com.ymall.backend.member.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ymall.backend.admin.entity.AdminGrade;

@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private LocalDateTime emailVerifiedAt;

    @Column(length = 100)
    private String password;

    public boolean hasPassword() {
        return password != null;
    }

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AdminGrade adminGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberAccessStatus accessStatus;

    private LocalDateTime lastLoginAt;

    @Column(length = 500)
    private String restrictionReason;

    private LocalDateTime restrictedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restricted_by")
    private Member restrictedBy;

    @Column(nullable = false)
    private long authVersion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Member(String email, String password, String name, MemberRole role) {
        this(email, password, name, null, role);
    }

    public Member(String email, String password, String name, String phone, MemberRole role) {
        this.email = email;
        this.emailVerifiedAt = LocalDateTime.now();
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.adminGrade = role == MemberRole.ROLE_ADMIN ? AdminGrade.SUPER_ADMIN : null;
        this.accessStatus = MemberAccessStatus.ACTIVE;
        this.authVersion = 0L;
    }

    public void updateProfile(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void changeEmail(String email) {
        this.email = email;
        this.emailVerifiedAt = LocalDateTime.now();
    }

    public void promoteToSeller() {
        this.role = MemberRole.ROLE_SELLER;
        this.adminGrade = null;
        incrementAuthVersion();
    }

    public void changeAdminRole(MemberRole targetRole, AdminGrade targetGrade) {
        if (targetRole == MemberRole.ROLE_ADMIN && targetGrade == null) {
            throw new IllegalArgumentException("Admin grade is required for an admin member");
        }
        if (targetRole != MemberRole.ROLE_ADMIN && targetGrade != null) {
            throw new IllegalArgumentException("Admin grade is only allowed for an admin member");
        }
        this.role = targetRole;
        this.adminGrade = targetGrade;
        incrementAuthVersion();
    }

    public void restrict(Member actor, String reason) {
        this.accessStatus = MemberAccessStatus.RESTRICTED;
        this.restrictionReason = reason;
        this.restrictedAt = LocalDateTime.now();
        this.restrictedBy = actor;
        incrementAuthVersion();
    }

    public void restoreAccess() {
        this.accessStatus = MemberAccessStatus.ACTIVE;
        this.restrictionReason = null;
        this.restrictedAt = null;
        this.restrictedBy = null;
        incrementAuthVersion();
    }

    public void revokeSessions() {
        incrementAuthVersion();
    }

    private void incrementAuthVersion() {
        this.authVersion = Math.addExact(this.authVersion, 1L);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.emailVerifiedAt == null) {
            this.emailVerifiedAt = now;
        }
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
