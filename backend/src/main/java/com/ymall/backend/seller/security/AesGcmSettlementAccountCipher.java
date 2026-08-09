package com.ymall.backend.seller.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

/**
 * 판매자 정산 계좌를 인증 암호화하고 암호문 형식을 Version Prefix와 함께 저장한다.
 *
 * <p>매 암호화마다 새 96-bit IV를 사용하고 계좌 소유 Context를 AAD에 포함하여 암호문을 다른
 * 판매자나 필드에 재사용하지 못하게 한다. 암호화 키는 Base64로 주입된 256-bit Key만 허용하며
 * 저장소나 로그에 평문 또는 Key를 기록하지 않는다.</p>
 */
@Component
public class AesGcmSettlementAccountCipher implements SettlementAccountCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String VERSION_PREFIX = "v1:";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String encodedKey;

    public AesGcmSettlementAccountCipher(
        @Value("${ymall.settlement-account.encryption-key:}") String encodedKey
    ) {
        this.encodedKey = encodedKey;
    }

    @Override
    public String encrypt(String plaintext, String context) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, iv, context);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length)
                .put(iv)
                .put(encrypted)
                .array();
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(
                ErrorCode.SETTLEMENT_ACCOUNT_ENCRYPTION_FAILED,
                exception
            );
        }
    }

    @Override
    public String decrypt(String ciphertext, String context) {
        if (ciphertext == null || !ciphertext.startsWith(VERSION_PREFIX)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ACCOUNT_ENCRYPTION_FAILED);
        }
        try {
            byte[] payload = Base64.getDecoder().decode(
                ciphertext.substring(VERSION_PREFIX.length())
            );
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new GeneralSecurityException("Invalid encrypted payload");
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = createCipher(Cipher.DECRYPT_MODE, iv, context);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new BusinessException(
                ErrorCode.SETTLEMENT_ACCOUNT_ENCRYPTION_FAILED,
                exception
            );
        }
    }

    private Cipher createCipher(int mode, byte[] iv, String context)
        throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(
            mode,
            new SecretKeySpec(decodeKey(), "AES"),
            new GCMParameterSpec(TAG_LENGTH_BITS, iv)
        );
        cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private byte[] decodeKey() {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new BusinessException(
                ErrorCode.SETTLEMENT_ACCOUNT_SECURITY_CONFIGURATION_ERROR
            );
        }
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != KEY_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid encryption key length");
            }
            return key;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                ErrorCode.SETTLEMENT_ACCOUNT_SECURITY_CONFIGURATION_ERROR,
                exception
            );
        }
    }
}
