package com.spendsense.api.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class DeploymentStartupValidator implements ApplicationRunner {
    private final SpendSenseProperties properties;
    private final Environment environment;

    public DeploymentStartupValidator(SpendSenseProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> failures = new ArrayList<>();
        requireSecureUrl("PUBLIC_BASE_URL", properties.api().publicBaseUrl(), failures);
        requireSecureUrl("SUPABASE_JWT_ISSUER", properties.security().supabase().issuer(), failures);
        validateDatabase(failures);
        validateSupabaseSecrets(failures);
        validateCors(failures);
        validateDeliverySecrets(failures);

        if (!failures.isEmpty()) {
            throw new IllegalStateException("Production deployment validation failed: " + String.join("; ", failures));
        }
    }

    private void validateDatabase(List<String> failures) {
        String databaseUrl = environment.getProperty("spring.datasource.url", "");
        if (databaseUrl.contains("localhost") || databaseUrl.contains("127.0.0.1")) {
            failures.add("DATABASE_URL must not point to localhost in production");
        }
        String password = environment.getProperty("spring.datasource.password", "");
        if (password.isBlank() || password.contains("local-password")) {
            failures.add("DATABASE_PASSWORD must be provided from managed secrets");
        }
    }

    private void validateSupabaseSecrets(List<String> failures) {
        SpendSenseProperties.Supabase supabase = properties.security().supabase();
        boolean hasJwks = supabase.jwksUri() != null && !supabase.jwksUri().isBlank();
        boolean hasJwtSecret = supabase.jwtSecret() != null && !supabase.jwtSecret().isBlank();
        if (!hasJwks && !hasJwtSecret) {
            failures.add("SUPABASE_JWKS_URI or SUPABASE_JWT_SECRET is required");
        }
        if (hasJwks) {
            requireSecureUrl("SUPABASE_JWKS_URI", supabase.jwksUri(), failures);
        }
    }

    private void validateCors(List<String> failures) {
        List<String> origins = properties.cors().allowedOrigins();
        if (origins == null || origins.isEmpty()) {
            failures.add("WEB_ORIGIN must be configured for production CORS");
            return;
        }
        for (String origin : origins) {
            if (origin == null || origin.isBlank() || origin.equals("*")) {
                failures.add("CORS origins must be explicit");
                continue;
            }
            requireSecureUrl("WEB_ORIGIN", origin, failures);
        }
    }

    private void validateDeliverySecrets(List<String> failures) {
        SpendSenseProperties.Webhooks webhooks = properties.delivery().webhooks();
        if (Boolean.TRUE.equals(webhooks.requireSignature())) {
            requireSecret("RESEND_WEBHOOK_SECRET", webhooks.resendSecret(), failures);
            requireSecret("SMTP_FALLBACK_WEBHOOK_SECRET", webhooks.smtpFallbackSecret(), failures);
            requireSecret("PUSH_PROVIDER_WEBHOOK_SECRET", webhooks.pushProviderSecret(), failures);
        }

        SpendSenseProperties.Resend resend = properties.delivery().email().resend();
        if (Boolean.TRUE.equals(resend.enabled())) {
            requireSecret("RESEND_API_KEY", resend.apiKey(), failures);
        }
    }

    private void requireSecureUrl(String name, String value, List<String> failures) {
        if (value == null || value.isBlank()) {
            failures.add(name + " is required");
            return;
        }
        if (!value.startsWith("https://")) {
            failures.add(name + " must use https");
        }
        if (value.contains("localhost") || value.contains("127.0.0.1")) {
            failures.add(name + " must not point to localhost");
        }
    }

    private void requireSecret(String name, String value, List<String> failures) {
        if (value == null || value.isBlank() || value.startsWith("your-") || value.contains("local")) {
            failures.add(name + " must be provided from managed secrets");
        }
    }
}
