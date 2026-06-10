package com.spendsense.api.dto.engagement;

import java.time.LocalTime;

public record NotificationPreferenceRequest(
        Boolean inAppEnabled,
        Boolean budgetWarningsEnabled,
        Boolean recurringRemindersEnabled,
        Boolean reportReadyEnabled,
        Boolean savingsNudgesEnabled,
        Boolean spendingIncreaseEnabled,
        Boolean weeklyDigestEnabled,
        Boolean monthlyReportEnabled,
        String timezone,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
}
