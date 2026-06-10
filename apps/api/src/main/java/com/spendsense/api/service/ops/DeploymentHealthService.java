package com.spendsense.api.service.ops;

import com.spendsense.api.config.SpendSenseProperties;
import com.spendsense.api.dto.ops.DeploymentHealthResponse;
import com.spendsense.api.dto.ops.ReleaseMetadataResponse;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeploymentHealthService {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final SpendSenseProperties properties;
    private final ObjectProvider<BuildProperties> buildProperties;
    private final Clock clock;

    public DeploymentHealthService(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            SpendSenseProperties properties,
            ObjectProvider<BuildProperties> buildProperties
    ) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.buildProperties = buildProperties;
        this.clock = Clock.systemUTC();
    }

    public DeploymentHealthResponse liveness() {
        return new DeploymentHealthResponse(
                "UP",
                "spendsense-api",
                operations().environment(),
                version(),
                commit(),
                maintenanceMode(),
                degradedMode(),
                Instant.now(clock),
                Map.of("application", "UP", "maintenance", maintenanceMode() ? "MAINTENANCE" : "UP")
        );
    }

    public DeploymentHealthResponse readiness() {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("application", "UP");
        checks.put("database", databaseStatus());
        checks.put("cors", corsStatus());
        checks.put("supabaseJwt", supabaseJwtStatus());
        checks.put("workerHeartbeat", workerHeartbeatStatus());
        checks.put("queueRecovery", queueRecoveryStatus());
        checks.put("providerConnectivity", providerConnectivityStatus());
        checks.put("webhookSignatures", webhookSignatureStatus());
        checks.put("operationalTraces", operationalTraceStatus());
        checks.put("maintenance", maintenanceMode() ? "MAINTENANCE" : "UP");
        checks.put("degraded", degradedMode() ? "DEGRADED" : "UP");
        String status = checks.values().stream().anyMatch("DEGRADED"::equals) ? "DEGRADED" : "UP";
        if ("DOWN".equals(checks.get("database")) || "DOWN".equals(checks.get("cors")) || "DOWN".equals(checks.get("supabaseJwt"))) {
            status = "DOWN";
        }
        if (maintenanceMode()) {
            status = "MAINTENANCE";
        } else if (degradedMode() && "UP".equals(status)) {
            status = "DEGRADED";
        }
        return new DeploymentHealthResponse(
                status,
                "spendsense-api",
                operations().environment(),
                version(),
                commit(),
                maintenanceMode(),
                degradedMode(),
                Instant.now(clock),
                checks
        );
    }

    public ReleaseMetadataResponse releaseMetadata() {
        SpendSenseProperties.Operations operations = operations();
        return new ReleaseMetadataResponse(
                "spendsense-api",
                operations.environment(),
                version(),
                commit(),
                maintenanceMode(),
                degradedMode(),
                operations.featureFlags(),
                operations.alertEscalationEmail(),
                Instant.now(clock)
        );
    }

    private String databaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception exception) {
            return "DOWN";
        }
    }

    private String corsStatus() {
        return properties.cors().allowedOrigins() == null || properties.cors().allowedOrigins().isEmpty()
                ? "DOWN"
                : "UP";
    }

    private String supabaseJwtStatus() {
        SpendSenseProperties.Supabase supabase = properties.security().supabase();
        boolean hasIssuer = supabase.issuer() != null && !supabase.issuer().isBlank();
        boolean hasJwks = supabase.jwksUri() != null && !supabase.jwksUri().isBlank();
        boolean hasSecret = supabase.jwtSecret() != null && !supabase.jwtSecret().isBlank();
        if (!hasIssuer || (!hasJwks && !hasSecret)) {
            return operations().environment().equals("staging") || operations().environment().equals("production")
                    ? "DOWN"
                    : "WAITING";
        }
        return hasIssuer && (hasJwks || hasSecret) ? "UP" : "DOWN";
    }

    private String workerHeartbeatStatus() {
        try {
            Instant heartbeat = jdbcTemplate.query("""
                    select heartbeat_at from worker_job_logs
                    order by heartbeat_at desc
                    limit 1
                    """, rs -> rs.next() ? rs.getTimestamp("heartbeat_at").toInstant() : null);
            if (heartbeat == null) {
                return "WAITING";
            }
            return heartbeat.isBefore(Instant.now(clock).minusSeconds(1800)) ? "DEGRADED" : "UP";
        } catch (Exception exception) {
            return "WAITING";
        }
    }

    private String queueRecoveryStatus() {
        try {
            Long expiredLocks = jdbcTemplate.queryForObject("""
                    select count(*) from worker_queues
                    where status = 'RUNNING' and locked_until < current_timestamp
                    """, Long.class);
            Long deadLetters = jdbcTemplate.queryForObject("""
                    select count(*) from worker_queues where status = 'DEAD_LETTER'
                    """, Long.class);
            if ((expiredLocks != null && expiredLocks > 0) || (deadLetters != null && deadLetters > 0)) {
                return "DEGRADED";
            }
            return "UP";
        } catch (Exception exception) {
            return "WAITING";
        }
    }

    private String providerConnectivityStatus() {
        SpendSenseProperties.Email email = properties.delivery().email();
        boolean resendEnabled = Boolean.TRUE.equals(email.resend().enabled());
        boolean smtpEnabled = Boolean.TRUE.equals(email.smtp().enabled());
        if (!resendEnabled && !smtpEnabled) {
            return "WAITING";
        }
        if (resendEnabled && (email.resend().apiKey() == null || email.resend().apiKey().isBlank())) {
            return "DOWN";
        }
        return "UP";
    }

    private String webhookSignatureStatus() {
        SpendSenseProperties.Webhooks webhooks = properties.delivery().webhooks();
        if (!Boolean.TRUE.equals(webhooks.requireSignature())) {
            return operations().environment().equals("local") || operations().environment().equals("development")
                    ? "WAITING"
                    : "DEGRADED";
        }
        boolean configured = hasText(webhooks.resendSecret()) || hasText(webhooks.smtpFallbackSecret()) || hasText(webhooks.pushProviderSecret());
        return configured ? "UP" : "DOWN";
    }

    private String operationalTraceStatus() {
        try {
            jdbcTemplate.queryForObject("select count(*) from operational_trace_events", Long.class);
            return "UP";
        } catch (Exception exception) {
            return "WAITING";
        }
    }

    private String version() {
        String configuredVersion = operations().releaseVersion();
        if (configuredVersion != null && !configuredVersion.isBlank()) {
            return configuredVersion;
        }
        BuildProperties build = buildProperties.getIfAvailable();
        return build == null ? "0.0.1-SNAPSHOT" : build.getVersion();
    }

    private String commit() {
        String commit = operations().releaseCommit();
        return commit == null || commit.isBlank() ? "local" : commit;
    }

    private boolean maintenanceMode() {
        return Boolean.TRUE.equals(operations().maintenanceMode());
    }

    private boolean degradedMode() {
        return Boolean.TRUE.equals(operations().degradedMode());
    }

    private SpendSenseProperties.Operations operations() {
        return properties.operations();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
