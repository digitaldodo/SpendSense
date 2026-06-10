package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record DeliveryTimelineEventResponse(
        UUID id,
        UUID notificationDeliveryId,
        String source,
        String provider,
        String eventType,
        String status,
        String message,
        Instant observedAt
) {
}
