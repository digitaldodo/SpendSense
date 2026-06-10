package com.spendsense.api.dto.delivery;

import java.util.UUID;

public record WebhookIngestionResponse(
        UUID webhookEventId,
        String provider,
        String eventType,
        String normalizedStatus,
        boolean signatureValid,
        boolean duplicateEvent,
        boolean deliverySynced,
        UUID notificationDeliveryId
) {
}
