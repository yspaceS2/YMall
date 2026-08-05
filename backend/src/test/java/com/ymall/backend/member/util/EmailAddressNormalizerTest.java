package com.ymall.backend.member.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailAddressNormalizerTest {

    @Test
    void trimsAndLowercasesEmailAddress() {
        assertThat(EmailAddressNormalizer.normalize(" User@Example.COM "))
            .isEqualTo("user@example.com");
    }
}
