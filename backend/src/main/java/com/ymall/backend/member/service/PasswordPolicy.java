package com.ymall.backend.member.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;

@Component
public class PasswordPolicy {

    private static final int SHORT_PASSWORD_MIN_LENGTH = 8;
    private static final int LONG_PASSWORD_MIN_LENGTH = 10;
    private static final int SHORT_PASSWORD_REQUIRED_TYPES = 3;
    private static final int LONG_PASSWORD_REQUIRED_TYPES = 2;
    private static final int SEQUENTIAL_RUN_LENGTH = 4;
    private static final int REPEATED_RUN_LENGTH = 4;
    private static final Set<String> WEAK_PASSWORDS = Set.of(
        "abcd",
        "1234",
        "1111",
        "test",
        "password",
        "public",
        "admin",
        "administrator",
        "manager",
        "guest",
        "tomcat",
        "root",
        "user",
        "operator",
        "anonymous"
    );

    public void validate(String password, String email) {
        String normalizedPassword = password.toLowerCase(Locale.ROOT);
        if (!hasRequiredComplexity(password)
            || WEAK_PASSWORDS.contains(normalizedPassword)
            || containsSequentialRun(normalizedPassword)
            || containsRepeatedRun(normalizedPassword)
            || resemblesEmailIdentifier(normalizedPassword, email)) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD);
        }
    }

    private boolean hasRequiredComplexity(String password) {
        int length = password.codePointCount(0, password.length());
        int characterTypes = countCharacterTypes(password);
        return (length >= SHORT_PASSWORD_MIN_LENGTH
            && characterTypes >= SHORT_PASSWORD_REQUIRED_TYPES)
            || (length >= LONG_PASSWORD_MIN_LENGTH
            && characterTypes >= LONG_PASSWORD_REQUIRED_TYPES);
    }

    private int countCharacterTypes(String password) {
        boolean hasUppercase = password.codePoints().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.codePoints().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.codePoints().anyMatch(Character::isDigit);
        boolean hasSpecial = password.codePoints().anyMatch(codePoint -> !Character.isLetterOrDigit(codePoint));
        return count(hasUppercase) + count(hasLowercase) + count(hasDigit) + count(hasSpecial);
    }

    private int count(boolean present) {
        return present ? 1 : 0;
    }

    private boolean containsSequentialRun(String password) {
        int ascending = 1;
        int descending = 1;
        for (int index = 1; index < password.length(); index++) {
            char previous = password.charAt(index - 1);
            char current = password.charAt(index);
            if (!Character.isLetterOrDigit(previous) || !Character.isLetterOrDigit(current)) {
                ascending = 1;
                descending = 1;
                continue;
            }
            ascending = current == previous + 1 ? ascending + 1 : 1;
            descending = current == previous - 1 ? descending + 1 : 1;
            if (ascending >= SEQUENTIAL_RUN_LENGTH || descending >= SEQUENTIAL_RUN_LENGTH) {
                return true;
            }
        }
        return false;
    }

    private boolean containsRepeatedRun(String password) {
        int repeated = 1;
        for (int index = 1; index < password.length(); index++) {
            repeated = password.charAt(index) == password.charAt(index - 1) ? repeated + 1 : 1;
            if (repeated >= REPEATED_RUN_LENGTH) {
                return true;
            }
        }
        return false;
    }

    private boolean resemblesEmailIdentifier(String password, String email) {
        if (email == null) {
            return false;
        }
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        int separatorIndex = normalizedEmail.indexOf('@');
        String identifier = separatorIndex < 0
            ? normalizedEmail
            : normalizedEmail.substring(0, separatorIndex);
        return identifier.length() >= 4 && password.contains(identifier);
    }
}
