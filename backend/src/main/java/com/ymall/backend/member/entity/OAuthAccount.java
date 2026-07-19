package com.ymall.backend.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "oauth_accounts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_oauth_accounts_provider_user",
            columnNames = {"provider", "provider_user_id"}
        ),
        @UniqueConstraint(
            name = "uk_oauth_accounts_member_provider",
            columnNames = {"member_id", "provider"}
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    public OAuthAccount(Member member, OAuthProvider provider, String providerUserId) {
        this.member = member;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }
}
