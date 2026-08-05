package com.ymall.backend.member.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.ymall.backend.member.config.PasswordResetProperties;
import com.ymall.backend.member.dto.PasswordResetConfirmRequest;
import com.ymall.backend.member.dto.PasswordResetRequestResponse;
import com.ymall.backend.member.dto.PasswordResetVerificationResponse;
import com.ymall.backend.member.entity.Member;
import com.ymall.backend.member.repository.MemberRepository;
import com.ymall.backend.member.util.EmailAddressNormalizer;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String CHALLENGE_KEY_PREFIX = "password-reset:challenge:";
    private static final String TOKEN_KEY_PREFIX = "password-reset:token:";
    private static final String RATE_KEY_PREFIX = "password-reset:rate:";
    private static final String COOLDOWN_KEY_PREFIX = "password-reset:cooldown:";
    private static final String DUMMY_MEMBER_ID = "0";
    private static final DefaultRedisScript<String> VERIFY_SCRIPT = new DefaultRedisScript<>(
        """
        local storedDigest = redis.call('HGET', KEYS[1], 'codeDigest')
        if not storedDigest then
            return nil
        end
        local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
        if attempts > tonumber(ARGV[2]) then
            redis.call('DEL', KEYS[1])
            return nil
        end
        if storedDigest ~= ARGV[1] then
            if attempts >= tonumber(ARGV[2]) then
                redis.call('DEL', KEYS[1])
            end
            return nil
        end
        local memberId = redis.call('HGET', KEYS[1], 'memberId')
        redis.call('DEL', KEYS[1])
        if not memberId or memberId == '0' then
            return nil
        end
        redis.call('SET', KEYS[2], memberId, 'EX', ARGV[3])
        return memberId
        """,
        String.class
    );

    private final StringRedisTemplate redisTemplate;
    private final MemberMailSender memberMailSender;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetProperties properties;

    public PasswordResetRequestResponse request(String requestedEmail) {
        String email = EmailAddressNormalizer.normalize(requestedEmail);
        String emailHash = SecurityTokenUtils.sha256(email);
        enforceRequestLimits(emailHash);

        String requestId = SecurityTokenUtils.generateUrlSafeToken();
        String code = SecurityTokenUtils.generateSixDigitCode();
        String challengeKey = CHALLENGE_KEY_PREFIX + SecurityTokenUtils.sha256(requestId);
        String memberId = memberRepository.findByEmailIgnoreCase(email)
            .filter(Member::hasPassword)
            .map(member -> member.getId().toString())
            .orElse(DUMMY_MEMBER_ID);

        redisTemplate.opsForHash().putAll(challengeKey, Map.of(
            "memberId", memberId,
            "codeDigest", SecurityTokenUtils.sha256(requestId + ":" + code),
            "attempts", "0"
        ));
        redisTemplate.expire(challengeKey, properties.getCodeTtl());

        if (!DUMMY_MEMBER_ID.equals(memberId) && !sendEmail(email, code)) {
            redisTemplate.delete(challengeKey);
        }
        return new PasswordResetRequestResponse(requestId);
    }

    public PasswordResetVerificationResponse verify(String requestId, String code) {
        String resetToken = SecurityTokenUtils.generateUrlSafeToken();
        String memberId = redisTemplate.execute(
            VERIFY_SCRIPT,
            List.of(
                CHALLENGE_KEY_PREFIX + SecurityTokenUtils.sha256(requestId),
                TOKEN_KEY_PREFIX + SecurityTokenUtils.sha256(resetToken)
            ),
            SecurityTokenUtils.sha256(requestId + ":" + code),
            Integer.toString(properties.getMaxAttempts()),
            Long.toString(properties.getResetTokenTtl().toSeconds())
        );
        if (memberId == null) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_VERIFICATION_FAILED);
        }
        return new PasswordResetVerificationResponse(
            resetToken,
            properties.getResetTokenTtl().toSeconds()
        );
    }

    @Transactional
    public void reset(PasswordResetConfirmRequest request) {
        String memberId = redisTemplate.opsForValue().getAndDelete(
            TOKEN_KEY_PREFIX + SecurityTokenUtils.sha256(request.resetToken())
        );
        if (memberId == null) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        Member member;
        try {
            member = memberRepository.findByIdForUpdate(Long.valueOf(memberId))
                .filter(Member::hasPassword)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        member.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(member.getId());
    }

    private void enforceRequestLimits(String emailHash) {
        String rateKey = RATE_KEY_PREFIX + emailHash;
        Long requestCount = redisTemplate.opsForValue().increment(rateKey);
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(rateKey, properties.getRequestWindow());
        }

        Boolean cooldownStarted = redisTemplate.opsForValue().setIfAbsent(
            COOLDOWN_KEY_PREFIX + emailHash,
            "1",
            properties.getResendInterval()
        );
        if (requestCount == null
            || requestCount > properties.getMaxRequests()
            || !Boolean.TRUE.equals(cooldownStarted)) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_REQUEST_LIMIT_EXCEEDED);
        }
    }

    private boolean sendEmail(String email, String code) {
        try {
            memberMailSender.send(
                email,
                "[YMall] 비밀번호 재설정 인증번호",
                "YMall 비밀번호 재설정 인증번호는 " + code + "입니다. 유효 시간 안에 입력해 주세요."
            );
            return true;
        } catch (MailException exception) {
            log.warn("Password reset email delivery failed");
            return false;
        }
    }

}
