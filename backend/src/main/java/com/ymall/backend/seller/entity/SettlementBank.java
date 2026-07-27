package com.ymall.backend.seller.entity;

import java.util.Arrays;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

public enum SettlementBank {
    KB_KOOKMIN("004", "KB국민은행"),
    SHINHAN("088", "신한은행"),
    HANA("081", "하나은행"),
    WOORI("020", "우리은행"),
    NH("011", "NH농협은행"),
    IBK("003", "IBK기업은행"),
    KAKAO_BANK("090", "카카오뱅크"),
    TOSS_BANK("092", "토스뱅크");

    private final String code;
    private final String displayName;

    SettlementBank(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SettlementBank fromCode(String code) {
        return Arrays.stream(values())
            .filter(bank -> bank.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_BANK_NOT_SUPPORTED));
    }
}
