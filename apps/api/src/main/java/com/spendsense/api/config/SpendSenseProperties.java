package com.spendsense.api.config;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spendsense")
public record SpendSenseProperties(
        Api api,
        Security security,
        Cors cors,
        Demo demo,
        Delivery delivery,
        Ai ai,
        Performance performance,
        Operations operations
) {
    public record Api(@NotBlank String version, @NotBlank String publicBaseUrl) {
    }

    public record Security(Supabase supabase, Headers headers, RateLimit rateLimit) {
    }

    public record Supabase(
            @NotBlank String issuer,
            @NotBlank String audience,
            String jwksUri,
            String jwtSecret
    ) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Demo(Boolean enabled) {
    }

    public record Delivery(Worker worker, Email email, Webhooks webhooks) {
    }

    public record Worker(Integer batchSize, Integer retryDelaySeconds, Integer lockTtlSeconds, Integer cleanupRetentionDays) {
    }

    public record Email(String primaryProvider, Resend resend, Smtp smtp) {
    }

    public record Resend(Boolean enabled, String apiKey, String fromEmail, String fromName) {
    }

    public record Smtp(Boolean enabled, String fromEmail, String fromName) {
    }

    public record Webhooks(Boolean requireSignature, String resendSecret, String smtpFallbackSecret, String pushProviderSecret, Integer replayWindowSeconds) {
    }

    public record Ai(String provider, String model, Integer timeoutMs, Integer maxContextItems, Integer maxOutputTokens, BigDecimalCost estimatedCostPerThousandTokens) {
    }

    public record BigDecimalCost(java.math.BigDecimal input, java.math.BigDecimal output, String currency) {
    }

    public record Headers(String contentSecurityPolicy, String permissionsPolicy) {
    }

    public record RateLimit(Boolean enabled, Integer requestsPerMinute, Integer burstCapacity) {
    }

    public record Performance(Long slowRequestThresholdMs) {
    }

    public record Operations(
            @NotBlank String environment,
            String releaseVersion,
            String releaseCommit,
            Boolean maintenanceMode,
            Boolean degradedMode,
            String featureFlags,
            String alertEscalationEmail
    ) {
    }
}
