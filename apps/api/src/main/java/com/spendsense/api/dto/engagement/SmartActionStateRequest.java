package com.spendsense.api.dto.engagement;

import java.time.Instant;

public record SmartActionStateRequest(
        String reason,
        Instant snoozedUntil
) {
}
