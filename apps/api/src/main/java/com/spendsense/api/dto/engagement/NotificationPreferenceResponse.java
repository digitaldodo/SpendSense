package com.spendsense.api.dto.engagement;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record NotificationPreferenceResponse(
        UUID id,
        boolean inAppEnabled,
        boolean budgetWarningsEnabled,
        boolean recurringRemindersEnabled,
        boolean reportReadyEnabled,
        boolean savingsNudgesEnabled,
        boolean spendingIncreaseEnabled,
        boolean weeklyDigestEnabled,
        boolean monthlyReportEnabled,
        String timezone,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd,
        Instant updatedAt
) {
}
