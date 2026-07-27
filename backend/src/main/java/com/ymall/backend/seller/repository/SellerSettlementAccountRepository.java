package com.ymall.backend.seller.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.seller.entity.SellerSettlementAccount;

public interface SellerSettlementAccountRepository
    extends JpaRepository<SellerSettlementAccount, Long> {

    Optional<SellerSettlementAccount> findBySellerProfileId(Long sellerProfileId);
}
