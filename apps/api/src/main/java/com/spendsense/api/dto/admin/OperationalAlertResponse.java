package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record OperationalAlertResponse(
        UUID id,
        String alertKey,
        String severity,
        String status,
        String title,
        String summary,
        String sourceType,
        String sourceId,
        String runbookSlug,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant acknowledgedAt,
        String acknowledgedByEmail,
        String acknowledgmentNote,
        Instant resolvedAt,
        String metadataJson
) {
}
