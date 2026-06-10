package com.spendsense.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ReleaseMetadataLogger implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ReleaseMetadataLogger.class);

    private final SpendSenseProperties properties;

    public ReleaseMetadataLogger(SpendSenseProperties properties) {
        this.properties = properties;
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
    }
}
