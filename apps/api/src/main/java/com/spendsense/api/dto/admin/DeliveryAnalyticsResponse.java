package com.spendsense.api.dto.admin;

public record DeliveryAnalyticsResponse(
        long deliveriesLast24h,
        long deliveredLast24h,
        long failedLast24h,
        long retryScheduled,
        double successRate,
        long retryExhaustedLast24h,
        Long averageProviderLatencyMs
) {
}
