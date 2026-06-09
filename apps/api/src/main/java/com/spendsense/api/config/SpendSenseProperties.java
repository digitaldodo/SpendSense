package com.spendsense.api.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spendsense")
public record SpendSenseProperties(Api api, Security security) {
    public record Api(@NotBlank String version, @NotBlank String publicBaseUrl) {
    }

    public record Security(Jwt jwt) {
    }

    public record Jwt(
            @NotBlank String issuer,
            @Min(1) long accessTokenTtlMinutes,
            @Min(1) long refreshTokenTtlDays
    ) {
    }
}
