package com.spendsense.api.dto.engagement;

import java.time.Instant;
import java.util.UUID;

public record DeliveryHistoryResponse(
        UUID id,
        UUID notificationId,
        UUID scheduledReportId,
        UUID generatedReportId,
        String deliveryKind,
        String channel,
        String provider,
        String recipient,
        String subject,
        String status,
        int attemptCount,
        Instant nextRetryAt,
        Instant lastAttemptAt,
        Instant deliveredAt,
        Instant failedAt,
        String errorCode,
        String errorMessage,
        Instant createdAt
) {
}
