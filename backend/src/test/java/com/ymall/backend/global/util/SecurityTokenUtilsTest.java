package com.ymall.backend.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecurityTokenUtilsTest {

    @Test
    void generatesUrlSafeTokenWithoutPadding() {
        String token = SecurityTokenUtils.generateUrlSafeToken();

        assertThat(token).hasSize(43);
        assertThat(token).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    void generatesSixDigitVerificationCode() {
        assertThat(SecurityTokenUtils.generateSixDigitCode()).matches("^[0-9]{6}$");
    }

    @Test
    void createsSha256HexDigest() {
        assertThat(SecurityTokenUtils.sha256("abc"))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
