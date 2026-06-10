package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record ProviderDeliveryEventResponse(
        UUID id,
        UUID notificationDeliveryId,
        String provider,
        String channel,
        String eventType,
        String status,
        String providerMessageId,
        Long latencyMs,
        String errorCode,
        String errorMessage,
        Instant observedAt
) {
}
