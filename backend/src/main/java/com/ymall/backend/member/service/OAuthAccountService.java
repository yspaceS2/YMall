package com.ymall.backend.member.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.dto.OAuthAccountResponse;
import com.ymall.backend.member.entity.OAuthProvider;
import com.ymall.backend.member.repository.OAuthAccountRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthAccountService {

    private final OAuthAccountRepository oAuthAccountRepository;

    public List<OAuthAccountResponse> getAccounts(Long memberId) {
        return oAuthAccountRepository.findAllByMemberIdOrderByProvider(memberId).stream()
            .map(account -> new OAuthAccountResponse(account.getProvider()))
            .toList();
    }

    public OAuthProvider getProvider(String providerName) {
        try {
            return OAuthProvider.fromRegistrationId(providerName);
        } catch (IllegalArgumentException exception) {
            throw new com.ymall.backend.global.exception.BusinessException(
                com.ymall.backend.global.exception.ErrorCode.INVALID_REQUEST
            );
        }
    }

    public void requireLinkedProvider(Long memberId, OAuthProvider provider) {
        if (!oAuthAccountRepository.existsByMemberIdAndProvider(memberId, provider)) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_OAUTH_ACCOUNT_MISMATCH);
        }
    }
}
