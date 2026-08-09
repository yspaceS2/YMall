package com.ymall.backend.member.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.member.entity.OAuthAccount;
import com.ymall.backend.member.entity.OAuthProvider;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(
        OAuthProvider provider,
        String providerUserId
    );

    List<OAuthAccount> findAllByMemberIdOrderByProvider(Long memberId);

    boolean existsByMemberIdAndProvider(Long memberId, OAuthProvider provider);
}
