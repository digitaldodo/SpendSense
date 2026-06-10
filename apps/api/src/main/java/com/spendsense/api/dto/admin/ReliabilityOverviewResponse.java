package com.spendsense.api.dto.admin;

import java.time.Instant;
import java.util.List;

public record ReliabilityOverviewResponse(
        String status,
        Instant observedAt,
        List<OperationalAlertResponse> alerts,
        List<IncidentLogResponse> incidents,
        List<ProviderWebhookEventResponse> webhookEvents,
        List<RunbookEntryResponse> runbooks
) {
}
