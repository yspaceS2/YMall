package com.ymall.backend.seller.verification;

import com.ymall.backend.seller.entity.SettlementAccountVerificationStatus;
import com.ymall.backend.seller.entity.SettlementBank;

public interface SettlementAccountVerifier {

    SettlementAccountVerificationStatus verify(
        SettlementBank bank,
        String accountHolder,
        String accountNumber
    );
}
