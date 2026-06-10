package com.spendsense.api.dto.admin;

import java.time.Instant;

public record ProviderStatusResponse(
        String provider,
        String channel,
        String status,
        long attemptsLast24h,
        long failuresLast24h,
        double successRate,
        Long averageLatencyMs,
        Instant lastEventAt,
        String lastErrorCode
) {
}
