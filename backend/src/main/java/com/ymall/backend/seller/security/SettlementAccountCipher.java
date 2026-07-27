package com.ymall.backend.seller.security;

public interface SettlementAccountCipher {

    String encrypt(String plaintext, String context);

    String decrypt(String ciphertext, String context);
}
