package com.spendsense.api.security;

import com.spendsense.api.config.SpendSenseProperties;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final SpendSenseProperties properties;

    public JwtTokenService(SpendSenseProperties properties) {
        this.properties = properties;
    }

    public boolean isStructurallyValid(String token) {
        return token != null && token.split("\\.").length == 3;
    }

    public String issuer() {
        return properties.security().jwt().issuer();
    }

    public Duration accessTokenTtl() {
        return Duration.ofMinutes(properties.security().jwt().accessTokenTtlMinutes());
    }

    public Duration refreshTokenTtl() {
        return Duration.ofDays(properties.security().jwt().refreshTokenTtlDays());
    }
}
