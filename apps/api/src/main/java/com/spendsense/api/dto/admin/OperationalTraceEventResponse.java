package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record OperationalTraceEventResponse(
        UUID id,
        String eventType,
        String severity,
        String environment,
        String releaseVersion,
        String releaseCommit,
        String source,
        String sourceId,
        String traceId,
        String message,
        String metadataJson,
        Instant observedAt
) {
}
