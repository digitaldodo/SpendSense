package com.spendsense.api.dto.engagement;

import java.time.Instant;
import java.util.List;

public record SystemStatusResponse(
        String status,
        Instant observedAt,
        Instant lastWorkerHeartbeatAt,
        long deliveriesLast24h,
        long failedDeliveriesLast24h,
        long pendingRetries,
        double deliverySuccessRate,
        List<WorkerJobLogResponse> recentJobs
) {
}
