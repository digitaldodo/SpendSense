package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminNotificationResponse(
        UUID id,
        String severity,
        String title,
        String body,
        String targetType,
        UUID targetId,
        Instant createdAt
) {
}
