package com.spendsense.api.dto.engagement;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String severity,
        String title,
        String body,
        String actionLabel,
        String actionUrl,
        String sourceType,
        String sourceId,
        String deliveryChannel,
        String lifecycleStatus,
        int priority,
        boolean read,
        Instant scheduledFor,
        Instant deliveredAt,
        Instant readAt,
        Instant dismissedAt,
        Instant expiresAt,
        Instant createdAt
) {
}
