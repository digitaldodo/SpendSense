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
import org.springframework.stereotype.Service;

@Service
public class DeploymentHealthService {
    private final DataSource dataSource;
    private final SpendSenseProperties properties;
    private final ObjectProvider<BuildProperties> buildProperties;
    private final Clock clock;

    public DeploymentHealthService(
            DataSource dataSource,
            SpendSenseProperties properties,
            ObjectProvider<BuildProperties> buildProperties
    ) {
        this.dataSource = dataSource;
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
        checks.put("maintenance", maintenanceMode() ? "MAINTENANCE" : "UP");
        checks.put("degraded", degradedMode() ? "DEGRADED" : "UP");
        String status = checks.values().stream().allMatch("UP"::equals) ? "UP" : "DEGRADED";
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

    private String version() {
        String configuredVersion = operations().releaseVersion();
        if (configuredVersion != null && !configuredVersion.isBlank()) {
            return configuredVersion;
        }
        BuildProperties properties = buildProperties.getIfAvailable();
        return properties == null ? "0.0.1-SNAPSHOT" : properties.getVersion();
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
}
