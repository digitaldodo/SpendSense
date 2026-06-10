package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record IncidentLogResponse(
        UUID id,
        String incidentKey,
        String severity,
        String status,
        String title,
        String summary,
        String primarySourceType,
        String primarySourceId,
        int alertCount,
        Instant openedAt,
        Instant lastEventAt,
        Instant acknowledgedAt,
        Instant resolvedAt,
        String metadataJson
) {
}
