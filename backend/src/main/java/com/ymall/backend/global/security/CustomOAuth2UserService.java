package com.ymall.backend.global.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.entity.OAuthProvider;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuthMemberService oAuthMemberService;
    private final OAuthFlowContext oAuthFlowContext;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        try {
            OAuth2User user = delegate.loadUser(request);
            OAuthProvider provider = OAuthProvider.fromRegistrationId(
                request.getClientRegistration().getRegistrationId()
            );
            OAuth2UserProfile profile = OAuth2UserProfileFactory.create(provider, user.getAttributes());
            Long linkMemberId = oAuthFlowContext.consumeLink(provider).orElse(null);
            Member member = oAuthMemberService.resolve(provider, profile, linkMemberId).member();

            return new YMallOAuth2User(
                member,
                member == null
                    ? java.util.List.of()
                    : java.util.List.of(new SimpleGrantedAuthority(member.getRole().name())),
                user.getAttributes(),
                request.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
                    .getUserNameAttributeName(),
                provider,
                profile
            );
        } catch (BusinessException exception) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(exception.getErrorCode().name()),
                exception.getMessage(),
                exception
            );
        }
    }

}
