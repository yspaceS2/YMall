package com.ymall.backend.seller.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

class AesGcmSettlementAccountCipherTest {

    @Test
    void encryptsWithRandomIvAndDecryptsOnlyInOriginalContext() {
        AesGcmSettlementAccountCipher cipher = new AesGcmSettlementAccountCipher(
            createEncryptionKey()
        );
        String context = "seller-settlement-account:1:account-number";
        String plaintext = "000000000001";

        String firstCiphertext = cipher.encrypt(plaintext, context);
        String secondCiphertext = cipher.encrypt(plaintext, context);

        assertThat(firstCiphertext)
            .startsWith("v1:")
            .isNotEqualTo(secondCiphertext)
            .doesNotContain(plaintext);
        assertThat(cipher.decrypt(firstCiphertext, context)).isEqualTo(plaintext);
        assertThatThrownBy(() -> cipher.decrypt(
            firstCiphertext,
            "seller-settlement-account:2:account-number"
        )).isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.SETTLEMENT_ACCOUNT_ENCRYPTION_FAILED);
    }

    @Test
    void rejectsMissingOrInvalidEncryptionKey() {
        AesGcmSettlementAccountCipher missingKeyCipher =
            new AesGcmSettlementAccountCipher("");
        AesGcmSettlementAccountCipher shortKeyCipher =
            new AesGcmSettlementAccountCipher(
                Base64.getEncoder().encodeToString(new byte[16])
            );

        assertThatThrownBy(() -> missingKeyCipher.encrypt("value", "context"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.SETTLEMENT_ACCOUNT_SECURITY_CONFIGURATION_ERROR);
        assertThatThrownBy(() -> shortKeyCipher.encrypt("value", "context"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.SETTLEMENT_ACCOUNT_SECURITY_CONFIGURATION_ERROR);
    }

    private String createEncryptionKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
