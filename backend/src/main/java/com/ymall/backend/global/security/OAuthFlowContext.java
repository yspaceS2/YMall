package com.ymall.backend.global.security;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.member.entity.OAuthProvider;

@Component
@RequiredArgsConstructor
public class OAuthFlowContext {

    private static final String SESSION_KEY = OAuthFlowContext.class.getName() + ".request";
    private static final String LINK_SESSION_KEY = OAuthFlowContext.class.getName() + ".link";
    private static final String EMAIL_SESSION_KEY = OAuthFlowContext.class.getName() + ".email";
    private static final String EMAIL_CHANGE_REAUTHENTICATION_SESSION_KEY =
        OAuthFlowContext.class.getName() + ".emailChangeReauthentication";

    private final Clock clock;

    public void startLink(HttpServletRequest request, Long memberId, OAuthProvider provider) {
        HttpSession session = request.getSession(true);
        session.removeAttribute(EMAIL_CHANGE_REAUTHENTICATION_SESSION_KEY);
        session.setAttribute(
            LINK_SESSION_KEY,
            new LinkRequest(memberId, provider, clock.instant().plus(5, ChronoUnit.MINUTES))
        );
    }

    public void startEmailChangeReauthentication(
        HttpServletRequest request,
        Long memberId,
        OAuthProvider provider
    ) {
        HttpSession session = request.getSession(true);
        session.removeAttribute(LINK_SESSION_KEY);
        session.setAttribute(
            EMAIL_CHANGE_REAUTHENTICATION_SESSION_KEY,
            new EmailChangeReauthentication(
                memberId,
                provider,
                false,
                clock.instant().plus(5, ChronoUnit.MINUTES)
            )
        );
    }

    public Optional<Long> getEmailChangeReauthenticationMemberId(OAuthProvider provider) {
        HttpSession session = currentSession();
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(EMAIL_CHANGE_REAUTHENTICATION_SESSION_KEY);
        if (!(value instanceof EmailChangeReauthentication reauthentication)) {
            return Optional.empty();
        }
        if (reauthentication.expiresAt().isBefore(clock.instant())) {
            session.removeAttribute(EMAIL_CHANGE_REAUTHENTICATION_SESSION_KEY);
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_REAUTHENTICATION_REQUIRED);
        }
        if (reauthentication.provider() != provider) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_OAUTH_ACCOUNT_MISMATCH);
        }
        return Optional.of(reauthentication.memberId());
    }

    public void completeEmailChangeReauthentication(Long memberId, OAuthProvider provider) {
        HttpSession session = currentSession();
        if (session == null
            || !(session.getAttribute(EMAIL_CHANGE_REAUTHENTICATION_SESSION_KEY)
                instanceof EmailChangeReauthentication reauthentication)
            || !reauthentication.memberId().equals(memberId)
            || reauthentication.provider() != provider
            || reauthentication.expiresAt().isBefore(clock.instant())) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_REAUTHENTICATION_REQUIRED);
        }
        session.setAttribute(
            EMAIL_CHANGE_REAUTHENTICATION_SESSION_KEY,
            new EmailChangeReauthentication(
                memberId,
                provider,
                true,
                reauthentication.expiresAt()
            )
        );
    }

    public boolean consumeCompletedEmailChangeReauthentication(
        Long memberId,
        OAuthProvider provider
    ) {
        HttpSession session = currentSession();
        if (session == null
            || !(session.getAttribute(EMAIL_CHANGE_REAUTHENTICATION_SESSION_KEY)
                instanceof EmailChangeReauthentication reauthentication)) {
            return false;
        }
        session.removeAttribute(EMAIL_CHANGE_REAUTHENTICATION_SESSION_KEY);
        return reauthentication.completed()
            && reauthentication.memberId().equals(memberId)
            && reauthentication.provider() == provider
            && !reauthentication.expiresAt().isBefore(clock.instant());
    }

    public boolean consumeEmailChangeReauthenticationFailure() {
        HttpSession session = currentSession();
        if (session == null
            || !(session.getAttribute(EMAIL_CHANGE_REAUTHENTICATION_SESSION_KEY)
                instanceof EmailChangeReauthentication)) {
            return false;
        }
        session.removeAttribute(EMAIL_CHANGE_REAUTHENTICATION_SESSION_KEY);
        return true;
    }

    public Optional<Long> consumeLink(OAuthProvider provider) {
        HttpSession session = currentSession();
        if (session == null || !(session.getAttribute(LINK_SESSION_KEY) instanceof LinkRequest link)) {
            return Optional.empty();
        }
        session.removeAttribute(LINK_SESSION_KEY);
        if (link.provider() != provider || link.expiresAt().isBefore(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(link.memberId());
    }

    private HttpSession currentSession() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
            .getRequestAttributes();
        return attributes == null ? null : attributes.getRequest().getSession(false);
    }

    public void start(HttpServletRequest request, OAuthProvider provider, OAuth2UserProfile profile) {
        request.getSession(true).setAttribute(
            SESSION_KEY,
            new SignupRequest(provider, profile, clock.instant().plus(5, ChronoUnit.MINUTES))
        );
    }

    public Optional<PendingSignup> get(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(SESSION_KEY) instanceof SignupRequest signup)) {
            return Optional.empty();
        }
        if (signup.expiresAt().isBefore(clock.instant())) {
            session.removeAttribute(SESSION_KEY);
            return Optional.empty();
        }
        return Optional.of(new PendingSignup(signup.provider(), signup.profile()));
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
            session.removeAttribute(EMAIL_SESSION_KEY);
        }
    }

    public void startEmailVerification(HttpServletRequest request, String email, String code) {
        request.getSession(true).setAttribute(
            EMAIL_SESSION_KEY,
            new EmailVerification(
                email,
                code,
                false,
                0,
                clock.instant().plus(5, ChronoUnit.MINUTES)
            )
        );
    }

    public boolean verifyEmail(HttpServletRequest request, String email, String code) {
        HttpSession session = request.getSession(false);
        if (session == null
            || !(session.getAttribute(EMAIL_SESSION_KEY) instanceof EmailVerification verification)
            || verification.expiresAt().isBefore(clock.instant())
            || verification.attempts() >= 5
            || !verification.email().equals(email)) {
            return false;
        }
        boolean verified = verification.code().equals(code);
        session.setAttribute(
            EMAIL_SESSION_KEY,
            new EmailVerification(
                verification.email(),
                verification.code(),
                verified,
                verification.attempts() + 1,
                verification.expiresAt()
            )
        );
        return verified;
    }

    public Optional<String> getVerifiedEmail(HttpServletRequest request, String email) {
        HttpSession session = request.getSession(false);
        if (session == null
            || !(session.getAttribute(EMAIL_SESSION_KEY) instanceof EmailVerification verification)
            || !verification.verified()
            || verification.expiresAt().isBefore(clock.instant())
            || !verification.email().equals(email)) {
            return Optional.empty();
        }
        return Optional.of(verification.email());
    }

    public record PendingSignup(OAuthProvider provider, OAuth2UserProfile profile) {
    }

    private record SignupRequest(
        OAuthProvider provider,
        OAuth2UserProfile profile,
        Instant expiresAt
    ) {
    }

    private record LinkRequest(Long memberId, OAuthProvider provider, Instant expiresAt) {
    }

    private record EmailChangeReauthentication(
        Long memberId,
        OAuthProvider provider,
        boolean completed,
        Instant expiresAt
    ) {
    }

    private record EmailVerification(
        String email,
        String code,
        boolean verified,
        int attempts,
        Instant expiresAt
    ) {
    }
}
