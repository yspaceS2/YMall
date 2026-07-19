package com.ymall.backend.global.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.OAuthProvider;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuthMemberService oAuthMemberService;
    private final OAuthFlowContext oAuthFlowContext;
    private final OidcUserService delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        try {
            OidcUser user = delegate.loadUser(request);
            OAuthProvider provider = OAuthProvider.fromRegistrationId(
                request.getClientRegistration().getRegistrationId()
            );
            OAuth2UserProfile profile = OAuth2UserProfileFactory.create(provider, user.getClaims());
            Long linkMemberId = oAuthFlowContext.consumeLink(provider).orElse(null);
            Member member = oAuthMemberService.resolve(provider, profile, linkMemberId).member();
            return new YMallOidcUser(member, user, provider, profile);
        } catch (BusinessException exception) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(exception.getErrorCode().name()),
                exception.getMessage(),
                exception
            );
        }
    }
}
