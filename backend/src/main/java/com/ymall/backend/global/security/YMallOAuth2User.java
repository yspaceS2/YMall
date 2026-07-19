package com.ymall.backend.global.security;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.OAuthProvider;

public record YMallOAuth2User(
    Member member,
    Collection<? extends GrantedAuthority> authorities,
    Map<String, Object> attributes,
    String nameAttributeKey,
    OAuthProvider provider,
    OAuth2UserProfile profile
) implements OAuth2User, YMallOAuthPrincipal {

    @Override
    public String getName() {
        Object value = attributes.get(nameAttributeKey);
        return value == null ? profile.providerUserId() : value.toString();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
