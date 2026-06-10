package com.spendsense.api.dto.engagement;

import java.time.Instant;
import java.util.UUID;

public record ScheduledReportResponse(
        UUID id,
        String reportType,
        String format,
        String cadence,
        String timezone,
        String deliveryChannel,
        Instant nextRunAt,
        Instant lastRunAt,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
