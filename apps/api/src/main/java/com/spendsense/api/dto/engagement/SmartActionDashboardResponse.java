package com.spendsense.api.dto.engagement;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SmartActionDashboardResponse(
        Instant generatedAt,
        DailySummary dailySummary,
        FinancialFocus todayFocus,
        List<SmartActionResponse> actions,
        List<HabitStreakResponse> streaks,
        WeeklyCheckIn weeklyCheckIn,
        List<FinancialMilestone> milestones,
        List<SmartReminder> reminders,
        List<BehaviorTimelineItem> behaviorTimeline,
        FinancialJourney journey
) {
    public record DailySummary(
            String headline,
            BigDecimal monthIncome,
            BigDecimal monthSpend,
            BigDecimal netCashflow,
            BigDecimal savingsRate,
            String tone,
            String explanation
    ) {
    }

    public record FinancialFocus(
            String title,
            String body,
            String focusType,
            BigDecimal impactAmount,
            String actionId
    ) {
    }

    public record SmartActionResponse(
            UUID id,
            String actionType,
            String category,
            String status,
            Integer priority,
            String title,
            String body,
            String explanation,
            BigDecimal impactAmount,
            BigDecimal impactPercent,
            String currency,
            String sourceType,
            String sourceId,
            LocalDate dueOn,
            Instant snoozedUntil,
            Instant completedAt,
            Instant dismissedAt,
            Instant generatedAt
    ) {
    }

    public record HabitStreakResponse(
            UUID id,
            String streakKey,
            String label,
            Integer currentCount,
            Integer bestCount,
            String unit,
            String state,
            LocalDate lastQualifiedOn,
            String explanation
    ) {
    }

    public record WeeklyCheckIn(
            UUID id,
            LocalDate weekStart,
            LocalDate weekEnd,
            String status,
            String headline,
            List<String> wins,
            List<String> focus,
            Instant generatedAt,
            Instant completedAt
    ) {
    }

    public record FinancialMilestone(
            String type,
            String title,
            String body,
            BigDecimal value,
            String state
    ) {
    }

    public record SmartReminder(
            String type,
            String title,
            String body,
            String actionId,
            Instant remindAt,
            String state
    ) {
    }

    public record BehaviorTimelineItem(
            String label,
            String body,
            LocalDate occurredOn,
            BigDecimal value,
            String state
    ) {
    }

    public record FinancialJourney(
            Integer score,
            String state,
            String headline,
            List<JourneyStep> steps
    ) {
    }

    public record JourneyStep(
            String label,
            String state,
            Integer progress,
            String explanation
    ) {
    }
}
