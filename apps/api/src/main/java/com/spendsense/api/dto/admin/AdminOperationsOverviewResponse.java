package com.spendsense.api.dto.admin;

import com.spendsense.api.dto.engagement.WorkerJobLogResponse;
import java.time.Instant;
import java.util.List;

public record AdminOperationsOverviewResponse(
        String status,
        Instant observedAt,
        DeliveryAnalyticsResponse deliveryAnalytics,
        List<QueueHealthResponse> queues,
        List<ProviderStatusResponse> providers,
        List<AdminNotificationResponse> notifications,
        List<WorkerJobLogResponse> recentJobs
) {
}
