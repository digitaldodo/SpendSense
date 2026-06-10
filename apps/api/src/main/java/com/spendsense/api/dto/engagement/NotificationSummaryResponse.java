package com.spendsense.api.dto.engagement;

import java.util.List;

public record NotificationSummaryResponse(
        long unreadCount,
        long activeCount,
        List<NotificationResponse> latest,
        List<NotificationResponse> timeline
) {
}
