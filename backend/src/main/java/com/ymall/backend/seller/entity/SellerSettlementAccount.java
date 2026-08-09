package com.ymall.backend.seller.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "seller_settlement_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerSettlementAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false, unique = true)
    private SellerProfile sellerProfile;

    @Column(nullable = false, length = 3)
    private String bankCode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String accountHolderCiphertext;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String accountNumberCiphertext;

    @Column(nullable = false, length = 4)
    private String accountNumberLast4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementAccountVerificationStatus verificationStatus;

    private Instant verifiedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public SellerSettlementAccount(
        SellerProfile sellerProfile,
        String bankCode,
        String accountHolderCiphertext,
        String accountNumberCiphertext,
        String accountNumberLast4,
        SettlementAccountVerificationStatus verificationStatus,
        Instant now
    ) {
        this.sellerProfile = sellerProfile;
        this.createdAt = now;
        update(
            bankCode,
            accountHolderCiphertext,
            accountNumberCiphertext,
            accountNumberLast4,
            verificationStatus,
            now
        );
    }

    public void update(
        String bankCode,
        String accountHolderCiphertext,
        String accountNumberCiphertext,
        String accountNumberLast4,
        SettlementAccountVerificationStatus verificationStatus,
        Instant now
    ) {
        this.bankCode = bankCode;
        this.accountHolderCiphertext = accountHolderCiphertext;
        this.accountNumberCiphertext = accountNumberCiphertext;
        this.accountNumberLast4 = accountNumberLast4;
        this.verificationStatus = verificationStatus;
        this.verifiedAt = verificationStatus == SettlementAccountVerificationStatus.VERIFIED
            ? now
            : null;
        this.updatedAt = now;
    }
}
