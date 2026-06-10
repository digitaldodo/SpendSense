package com.spendsense.api.dto.engagement;

import java.time.Instant;
import java.util.UUID;

public record ReportDeliveryLogResponse(
        UUID id,
        UUID scheduledReportId,
        UUID generatedReportId,
        String deliveryChannel,
        String status,
        Instant attemptedAt,
        Instant deliveredAt,
        String errorMessage
) {
}
