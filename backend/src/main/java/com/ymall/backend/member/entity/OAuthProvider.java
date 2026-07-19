package com.ymall.backend.member.entity;

public enum OAuthProvider {
    GOOGLE,
    KAKAO,
    NAVER;

    public static OAuthProvider fromRegistrationId(String registrationId) {
        return valueOf(registrationId.toUpperCase());
    }
}
