package com.ymall.backend.global.security;

public record OAuth2UserProfile(String providerUserId, String email, String name) {
}
