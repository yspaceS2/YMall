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
@ConfigurationProperties(prefix = "ymall.password-reset")
public class PasswordResetProperties {

    @NotNull
    @DurationMin(seconds = 1)
    private Duration codeTtl = Duration.ofMinutes(5);

    @NotNull
    @DurationMin(seconds = 1)
    private Duration resetTokenTtl = Duration.ofMinutes(10);

    @NotNull
    @DurationMin(seconds = 1)
    private Duration resendInterval = Duration.ofMinutes(1);

    @NotNull
    @DurationMin(seconds = 1)
    private Duration requestWindow = Duration.ofHours(1);

    @Min(1)
    private int maxRequests = 5;

    @Min(1)
    private int maxAttempts = 5;

    public Duration getCodeTtl() {
        return codeTtl;
    }

    public void setCodeTtl(Duration codeTtl) {
        this.codeTtl = codeTtl;
    }

    public Duration getResetTokenTtl() {
        return resetTokenTtl;
    }

    public void setResetTokenTtl(Duration resetTokenTtl) {
        this.resetTokenTtl = resetTokenTtl;
    }

    public Duration getResendInterval() {
        return resendInterval;
    }

    public void setResendInterval(Duration resendInterval) {
        this.resendInterval = resendInterval;
    }

    public Duration getRequestWindow() {
        return requestWindow;
    }

    public void setRequestWindow(Duration requestWindow) {
        this.requestWindow = requestWindow;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
