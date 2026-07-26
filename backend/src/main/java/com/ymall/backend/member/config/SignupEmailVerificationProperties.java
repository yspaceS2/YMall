package com.ymall.backend.member.config;

import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Component
@Validated
@ConfigurationProperties(prefix = "ymall.signup-email-verification")
public class SignupEmailVerificationProperties {

    @NotNull
    @DurationMin(seconds = 1)
    private Duration codeTtl = Duration.ofMinutes(5);

    @NotNull
    @DurationMin(seconds = 1)
    private Duration tokenTtl = Duration.ofMinutes(10);

    @Min(1)
    private int maxAttempts = 5;

    @Min(1)
    private int maxRequests = 5;

    @NotNull
    @DurationMin(seconds = 1)
    private Duration requestWindow = Duration.ofMinutes(30);

    @NotNull
    @DurationMin(seconds = 1)
    private Duration resendInterval = Duration.ofMinutes(1);

    public Duration getCodeTtl() {
        return codeTtl;
    }

    public void setCodeTtl(Duration codeTtl) {
        this.codeTtl = codeTtl;
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public Duration getRequestWindow() {
        return requestWindow;
    }

    public void setRequestWindow(Duration requestWindow) {
        this.requestWindow = requestWindow;
    }

    public Duration getResendInterval() {
        return resendInterval;
    }

    public void setResendInterval(Duration resendInterval) {
        this.resendInterval = resendInterval;
    }
}
