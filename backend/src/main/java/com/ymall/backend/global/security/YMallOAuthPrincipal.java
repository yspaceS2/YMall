package com.ymall.backend.global.security;

import org.springframework.security.oauth2.core.user.OAuth2User;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.OAuthProvider;

public interface YMallOAuthPrincipal extends OAuth2User {

    Member member();

    OAuthProvider provider();

    OAuth2UserProfile profile();
}
