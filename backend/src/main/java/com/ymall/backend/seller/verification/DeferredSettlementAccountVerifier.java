package com.ymall.backend.seller.verification;

import org.springframework.stereotype.Component;

import com.ymall.backend.seller.entity.SettlementAccountVerificationStatus;
import com.ymall.backend.seller.entity.SettlementBank;

@Component
public class DeferredSettlementAccountVerifier implements SettlementAccountVerifier {

    @Override
    public SettlementAccountVerificationStatus verify(
        SettlementBank bank,
        String accountHolder,
        String accountNumber
    ) {
        return SettlementAccountVerificationStatus.UNVERIFIED;
    }
}
