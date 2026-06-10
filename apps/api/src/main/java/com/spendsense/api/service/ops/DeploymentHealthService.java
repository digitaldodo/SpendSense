package com.spendsense.api.service.ops;

import com.spendsense.api.config.SpendSenseProperties;
import com.spendsense.api.dto.ops.DeploymentHealthResponse;
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
                version(),
                Instant.now(clock),
                Map.of("application", "UP")
        );
    }

    public DeploymentHealthResponse readiness() {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("application", "UP");
        checks.put("database", databaseStatus());
        checks.put("cors", corsStatus());
        String status = checks.values().stream().allMatch("UP"::equals) ? "UP" : "DEGRADED";
        return new DeploymentHealthResponse(
                "DEGRADED".equals(status) ? "DOWN" : status,
                "spendsense-api",
                version(),
                Instant.now(clock),
                checks
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
        BuildProperties properties = buildProperties.getIfAvailable();
        return properties == null ? "0.0.1-SNAPSHOT" : properties.getVersion();
    }
}
