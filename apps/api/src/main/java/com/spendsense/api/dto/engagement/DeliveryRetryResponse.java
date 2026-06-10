package com.spendsense.api.dto.engagement;

import java.time.Instant;
import java.util.UUID;

public record DeliveryRetryResponse(
        UUID id,
        UUID deliveryId,
        int attemptNumber,
        Instant scheduledFor,
        Instant attemptedAt,
        String status,
        String errorCode,
        String errorMessage
) {
}
