package com.spendsense.api.dto.ops;

import java.time.Instant;
import java.util.Map;

public record DeploymentHealthResponse(
        String status,
        String service,
        String version,
        Instant checkedAt,
        Map<String, String> checks
) {
}
