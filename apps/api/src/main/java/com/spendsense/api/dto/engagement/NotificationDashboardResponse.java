package com.spendsense.api.dto.engagement;

import java.util.List;

public record NotificationDashboardResponse(
        long unreadCount,
        List<NotificationResponse> upcomingSubscriptions,
        List<NotificationResponse> budgetWarnings,
        List<NotificationResponse> reminders,
        List<ScheduledReportResponse> scheduledReports,
        List<NotificationResponse> savingsNudges
) {
}
