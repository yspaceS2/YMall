package com.ymall.backend.member.service;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.ymall.backend.global.exception.BusinessException;
import com.ymall.backend.global.exception.ErrorCode;
import com.ymall.backend.global.security.RefreshTokenService;
import com.ymall.backend.global.util.SecurityTokenUtils;
import com.ymall.backend.member.config.EmailChangeProperties;
import com.ymall.backend.member.dto.EmailChangeReauthenticationResponse;
import com.ymall.backend.member.dto.EmailChangeVerificationResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.event.MemberEmailChangedEvent;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.member.util.EmailAddressNormalizer;

@Service
@RequiredArgsConstructor
public class MemberEmailChangeService {

    private static final String NEW_CHALLENGE_PREFIX = "email-change:new:";
    private static final String REAUTHENTICATION_PREFIX = "email-change:reauthenticated:";
    private static final String RATE_PREFIX = "email-change:rate:";
    private static final String COOLDOWN_PREFIX = "email-change:cooldown:";
    private static final String PASSWORD_ATTEMPT_PREFIX = "email-change:password-attempt:";
    private static final int MAX_PASSWORD_ATTEMPTS = 10;
    private static final String REAUTHENTICATION_REQUIRED = "reauthentication_required";
    private static final DefaultRedisScript<String> VERIFY_AND_CONSUME_SCRIPT =
        new DefaultRedisScript<>(
        """
        local reauthenticatedEmailDigest = redis.call('GET', KEYS[2])
        if not reauthenticatedEmailDigest or reauthenticatedEmailDigest ~= ARGV[5] then
            return 'reauthentication_required'
        end
        local storedMemberId = redis.call('HGET', KEYS[1], 'memberId')
        local storedEmailDigest = redis.call('HGET', KEYS[1], 'emailDigest')
        local storedCodeDigest = redis.call('HGET', KEYS[1], 'codeDigest')
        if not storedMemberId or storedMemberId ~= ARGV[2]
            or not storedEmailDigest or storedEmailDigest ~= ARGV[3]
            or not storedCodeDigest then
            return 'verification_failed'
        end
        local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
        if attempts > tonumber(ARGV[4]) then
            redis.call('DEL', KEYS[1])
            return 'verification_failed'
        end
        if storedCodeDigest ~= ARGV[1] then
            if attempts >= tonumber(ARGV[4]) then
                redis.call('DEL', KEYS[1])
            end
            return 'verification_failed'
        end
        redis.call('DEL', KEYS[1])
        redis.call('DEL', KEYS[2])
        return 'verified'
        """,
        String.class
    );

    private final StringRedisTemplate redisTemplate;
    private final MemberMailSender memberMailSender;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailChangeProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    public EmailChangeReauthenticationResponse reauthenticate(
        Long memberId,
        String currentPassword,
        String sessionBinding
    ) {
        Member member = findMember(memberId);
        if (member.hasPassword()) {
            enforcePasswordAttemptLimit(memberId);
            if (currentPassword == null
                || currentPassword.isBlank()
                || !passwordEncoder.matches(currentPassword, member.getPassword())) {
                throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
            }
            redisTemplate.delete(PASSWORD_ATTEMPT_PREFIX + memberId);
            markReauthenticated(member, sessionBinding);
            return new EmailChangeReauthenticationResponse(
                false,
                null,
                null,
                properties.getReauthenticationTtl().toSeconds()
            );
        }
        throw new BusinessException(ErrorCode.EMAIL_CHANGE_REAUTHENTICATION_REQUIRED);
    }

    public void requireOAuthReauthentication(Long memberId) {
        if (findMember(memberId).hasPassword()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    public void markOAuthReauthenticated(Long memberId, String sessionBinding) {
        markReauthenticated(findMember(memberId), sessionBinding);
    }

    public EmailChangeVerificationResponse sendNewEmailVerification(
        Long memberId,
        String requestedEmail,
        String sessionBinding
    ) {
        Member member = findMember(memberId);
        requireReauthentication(member, sessionBinding);
        String email = EmailAddressNormalizer.normalize(requestedEmail);
        validateNewEmail(member, email);
        enforceEmailRequestLimits("new", memberId, SecurityTokenUtils.sha256(email));

        String requestId = SecurityTokenUtils.generateUrlSafeToken();
        String code = SecurityTokenUtils.generateSixDigitCode();
        String challengeKey = NEW_CHALLENGE_PREFIX + SecurityTokenUtils.sha256(requestId);
        redisTemplate.opsForHash().putAll(challengeKey, Map.of(
            "memberId", memberId.toString(),
            "emailDigest", SecurityTokenUtils.sha256(email),
            "codeDigest", SecurityTokenUtils.sha256(requestId + ":" + code),
            "attempts", "0"
        ));
        redisTemplate.expire(challengeKey, properties.getCodeTtl());
        try {
            sendCode(email, code, "새 이메일");
        } catch (BusinessException exception) {
            redisTemplate.delete(challengeKey);
            throw exception;
        }
        return new EmailChangeVerificationResponse(
            requestId,
            properties.getCodeTtl().toSeconds()
        );
    }

    @Transactional
    public void change(
        Long memberId,
        String requestId,
        String requestedEmail,
        String code,
        String sessionBinding
    ) {
        String email = EmailAddressNormalizer.normalize(requestedEmail);
        Member member = memberRepository.findByIdForUpdate(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        String verificationResult = redisTemplate.execute(
            VERIFY_AND_CONSUME_SCRIPT,
            List.of(
                NEW_CHALLENGE_PREFIX + SecurityTokenUtils.sha256(requestId),
                reauthenticationKey(memberId, sessionBinding)
            ),
            SecurityTokenUtils.sha256(requestId + ":" + code),
            memberId.toString(),
            SecurityTokenUtils.sha256(email),
            Integer.toString(properties.getMaxAttempts()),
            SecurityTokenUtils.sha256(member.getEmail())
        );
        if (REAUTHENTICATION_REQUIRED.equals(verificationResult)) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_REAUTHENTICATION_REQUIRED);
        }
        if (!"verified".equals(verificationResult)) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_VERIFICATION_FAILED);
        }

        validateNewEmail(member, email);

        String previousEmail = member.getEmail();
        member.changeEmail(email);
        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        refreshTokenService.revokeAll(memberId);
        eventPublisher.publishEvent(new MemberEmailChangedEvent(memberId, previousEmail, email));
    }

    private void markReauthenticated(Member member, String sessionBinding) {
        redisTemplate.opsForValue().set(
            reauthenticationKey(member.getId(), sessionBinding),
            SecurityTokenUtils.sha256(member.getEmail()),
            properties.getReauthenticationTtl()
        );
    }

    private void requireReauthentication(Member member, String sessionBinding) {
        String emailDigest = redisTemplate.opsForValue()
            .get(reauthenticationKey(member.getId(), sessionBinding));
        if (!SecurityTokenUtils.sha256(member.getEmail()).equals(emailDigest)) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_REAUTHENTICATION_REQUIRED);
        }
    }

    private void validateNewEmail(Member member, String email) {
        if (member.getEmail().equalsIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_SAME_AS_CURRENT);
        }
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
    }

    private void enforcePasswordAttemptLimit(Long memberId) {
        String key = PASSWORD_ATTEMPT_PREFIX + memberId;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, properties.getRequestWindow());
        }
        if (attempts == null || attempts > MAX_PASSWORD_ATTEMPTS) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_REQUEST_LIMIT_EXCEEDED);
        }
    }

    private void enforceEmailRequestLimits(String purpose, Long memberId, String emailDigest) {
        String suffix = purpose + ":" + memberId + ":" + emailDigest;
        String rateKey = RATE_PREFIX + suffix;
        Long requestCount = redisTemplate.opsForValue().increment(rateKey);
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(rateKey, properties.getRequestWindow());
        }
        Boolean cooldownStarted = redisTemplate.opsForValue().setIfAbsent(
            COOLDOWN_PREFIX + suffix,
            "1",
            properties.getResendInterval()
        );
        if (requestCount == null
            || requestCount > properties.getMaxRequests()
            || !Boolean.TRUE.equals(cooldownStarted)) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_REQUEST_LIMIT_EXCEEDED);
        }
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void sendCode(String email, String code, String purpose) {
        try {
            memberMailSender.send(
                email,
                "[YMall] 이메일 변경 " + purpose + " 인증번호",
                "YMall 이메일 변경 인증번호는 " + code + "입니다. 유효 시간 안에 입력해 주세요."
            );
        } catch (MailException exception) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_DELIVERY_FAILED);
        }
    }

    private String reauthenticationKey(Long memberId, String sessionBinding) {
        if (sessionBinding == null || sessionBinding.isBlank()) {
            throw new BusinessException(ErrorCode.EMAIL_CHANGE_REAUTHENTICATION_REQUIRED);
        }
        return REAUTHENTICATION_PREFIX + memberId + ":"
            + SecurityTokenUtils.sha256(sessionBinding);
    }
}
