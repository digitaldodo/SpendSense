package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record ProviderWebhookEventResponse(
        UUID id,
        String provider,
        String channel,
        String eventType,
        String normalizedStatus,
        String providerEventId,
        String providerMessageId,
        boolean signatureValid,
        boolean duplicateEvent,
        UUID replayOfEventId,
        boolean deliverySynced,
        UUID notificationDeliveryId,
        String failureReason,
        Instant receivedAt,
        Instant processedAt
) {
}
