package com.spendsense.api.dto.ops;

import java.time.Instant;

public record ReleaseMetadataResponse(
        String service,
        String environment,
        String version,
        String commit,
        boolean maintenanceMode,
        boolean degradedMode,
        String featureFlags,
        String alertEscalationEmail,
        Instant reportedAt
) {
}
