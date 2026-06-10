package com.spendsense.api.config;

import com.spendsense.api.service.ops.OperationalTraceService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ReleaseMetadataLogger implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ReleaseMetadataLogger.class);

    private final SpendSenseProperties properties;
    private final OperationalTraceService operationalTraceService;

    public ReleaseMetadataLogger(SpendSenseProperties properties, OperationalTraceService operationalTraceService) {
        this.properties = properties;
        this.operationalTraceService = operationalTraceService;
    }

    @Override
    public void run(ApplicationArguments args) {
        SpendSenseProperties.Operations operations = properties.operations();
        log.info(
                "SpendSense API release booted env={} version={} commit={} maintenanceMode={} degradedMode={}",
                operations.environment(),
                operations.releaseVersion(),
                operations.releaseCommit(),
                operations.maintenanceMode(),
                operations.degradedMode()
        );
        operationalTraceService.record(
                "release_booted",
                Boolean.TRUE.equals(operations.degradedMode()) ? "WARNING" : "INFO",
                "release-metadata",
                operations.releaseCommit(),
                null,
                "SpendSense API release booted.",
                Map.of(
                        "maintenanceMode", Boolean.TRUE.equals(operations.maintenanceMode()),
                        "degradedMode", Boolean.TRUE.equals(operations.degradedMode())
                )
        );
    }
}
