package com.ymall.backend.member.dto;

public record GoogleOneTapLoginResponse(
    boolean signupRequired,
    TokenResponse token
) {

    public static GoogleOneTapLoginResponse authenticated(TokenResponse token) {
        return new GoogleOneTapLoginResponse(false, token);
    }

    public static GoogleOneTapLoginResponse requiresSignup() {
        return new GoogleOneTapLoginResponse(true, null);
    }
}
