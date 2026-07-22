package com.ymall.backend.payment.gateway.toss;

import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "ymall.payment.toss")
public class TossPaymentProperties {

    private String clientKey = "";
    private String secretKey = "";

    @NotBlank
    private String baseUrl = "https://api.tosspayments.com";

    @NotNull
    @DurationMin(millis = 100)
    private Duration connectTimeout = Duration.ofSeconds(3);

    @NotNull
    @DurationMin(millis = 100)
    private Duration readTimeout = Duration.ofSeconds(5);

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean hasCredentials() {
        return !clientKey.isBlank() && !secretKey.isBlank();
    }
}
