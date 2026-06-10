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
        Boolean emailEnabled,
        String emailAddress,
        String digestFrequency,
        Boolean budgetAlertEmailEnabled,
        Boolean recurringReminderEmailEnabled,
        Boolean reportEmailEnabled,
        Boolean deliveryFailureAlertsEnabled,
        String timezone,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
}
