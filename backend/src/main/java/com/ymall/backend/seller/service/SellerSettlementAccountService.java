package com.ymall.backend.seller.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.seller.dto.SellerSettlementAccountResponse;
import com.ymall.backend.seller.dto.SellerSettlementAccountUpsertRequest;
import com.ymall.backend.seller.entity.SellerProfile;
import com.ymall.backend.seller.entity.SellerSettlementAccount;
import com.ymall.backend.seller.entity.SettlementAccountVerificationStatus;
import com.ymall.backend.seller.entity.SettlementBank;
import com.ymall.backend.seller.repository.SellerSettlementAccountRepository;
import com.ymall.backend.seller.security.SettlementAccountCipher;
import com.ymall.backend.seller.verification.SettlementAccountVerifier;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerSettlementAccountService {

    private static final String CONTEXT_PREFIX = "seller-settlement-account:";

    private final SellerSettlementAccountRepository settlementAccountRepository;
    private final SellerProfileService sellerProfileService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final SettlementAccountCipher settlementAccountCipher;
    private final SettlementAccountVerifier settlementAccountVerifier;
    private final Clock clock;

    public SellerSettlementAccountResponse get(Long memberId) {
        SellerProfile profile = sellerProfileService.getProfileEntity(memberId);
        SellerSettlementAccount account = settlementAccountRepository
            .findBySellerProfileId(profile.getId())
            .orElseThrow(() -> new BusinessException(
                ErrorCode.SELLER_SETTLEMENT_ACCOUNT_NOT_FOUND
            ));
        return toResponse(account);
    }

    @Transactional
    public SellerSettlementAccountResponse upsert(
        Long memberId,
        SellerSettlementAccountUpsertRequest request
    ) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        verifyCurrentPassword(member, request.currentPassword());

        SellerProfile profile = sellerProfileService.getProfileEntityForUpdate(memberId);
        SettlementBank bank = SettlementBank.fromCode(request.bankCode());
        String accountHolder = request.accountHolder().trim();
        String accountNumber = request.accountNumber();
        Instant now = clock.instant();
        SettlementAccountVerificationStatus verificationStatus =
            settlementAccountVerifier.verify(bank, accountHolder, accountNumber);
        String holderCiphertext = settlementAccountCipher.encrypt(
            accountHolder,
            encryptionContext(profile.getId(), "account-holder")
        );
        String numberCiphertext = settlementAccountCipher.encrypt(
            accountNumber,
            encryptionContext(profile.getId(), "account-number")
        );
        String last4 = accountNumber.substring(accountNumber.length() - 4);

        SellerSettlementAccount account = settlementAccountRepository
            .findBySellerProfileId(profile.getId())
            .orElseGet(() -> new SellerSettlementAccount(
                profile,
                bank.getCode(),
                holderCiphertext,
                numberCiphertext,
                last4,
                verificationStatus,
                now
            ));
        if (account.getId() != null) {
            account.update(
                bank.getCode(),
                holderCiphertext,
                numberCiphertext,
                last4,
                verificationStatus,
                now
            );
        }
        return toResponse(settlementAccountRepository.save(account));
    }

    private void verifyCurrentPassword(Member member, String currentPassword) {
        if (!member.hasPassword()
            || !passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
    }

    private SellerSettlementAccountResponse toResponse(SellerSettlementAccount account) {
        SettlementBank bank = SettlementBank.fromCode(account.getBankCode());
        String accountHolder = settlementAccountCipher.decrypt(
            account.getAccountHolderCiphertext(),
            encryptionContext(account.getSellerProfile().getId(), "account-holder")
        );
        return new SellerSettlementAccountResponse(
            account.getId(),
            bank.getCode(),
            bank.getDisplayName(),
            accountHolder,
            "****" + account.getAccountNumberLast4(),
            account.getVerificationStatus(),
            account.getVerifiedAt(),
            account.getUpdatedAt()
        );
    }

    private String encryptionContext(Long sellerProfileId, String field) {
        return CONTEXT_PREFIX + sellerProfileId + ":" + field;
    }
}
