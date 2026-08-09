package com.ymall.backend.member.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @ParameterizedTest
    @ValueSource(strings = {"Secure123!", "longpassword1", "긴비밀번호13579!"})
    void acceptsPasswordsMeetingKisaComplexityExamples(String password) {
        assertThatCode(() -> passwordPolicy.validate(password, "member@example.com"))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "onlylowercase",
        "Short1!",
        "Abcd1234!",
        "Abc1111!",
        "memberSecure1!",
        "password"
    })
    void rejectsWeakPredictableOrIdentifierSimilarPasswords(String password) {
        assertThatThrownBy(() -> passwordPolicy.validate(password, "member@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting(exception -> ((BusinessException) exception).getErrorCode())
            .isEqualTo(ErrorCode.WEAK_PASSWORD);
    }
}
