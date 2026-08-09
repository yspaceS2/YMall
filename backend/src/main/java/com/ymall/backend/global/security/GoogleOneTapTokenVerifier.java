package com.ymall.backend.global.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.OAuthProvider;

@Service
public class GoogleOneTapTokenVerifier {

    private final JwtDecoder jwtDecoder;
    private final GoogleOneTapNonceService nonceService;

    public GoogleOneTapTokenVerifier(
        @Qualifier("googleOneTapJwtDecoder") JwtDecoder jwtDecoder,
        GoogleOneTapNonceService nonceService
    ) {
        this.jwtDecoder = jwtDecoder;
        this.nonceService = nonceService;
    }

    public OAuth2UserProfile verify(String credential) {
        try {
            Jwt jwt = jwtDecoder.decode(credential);
            if (!Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
                throw invalidToken();
            }
            OAuth2UserProfile profile = OAuth2UserProfileFactory.create(
                OAuthProvider.GOOGLE,
                jwt.getClaims()
            );
            nonceService.consume(jwt.getClaimAsString("nonce"));
            return profile;
        } catch (JwtException exception) {
            throw new BusinessException(ErrorCode.GOOGLE_ONE_TAP_TOKEN_INVALID, exception);
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.GOOGLE_ONE_TAP_TOKEN_INVALID) {
                throw exception;
            }
            throw invalidToken();
        }
    }

    private BusinessException invalidToken() {
        return new BusinessException(ErrorCode.GOOGLE_ONE_TAP_TOKEN_INVALID);
    }
}
