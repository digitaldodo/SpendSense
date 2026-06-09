package com.spendsense.api.dto.finance;

import com.spendsense.api.domain.finance.SavingsGoalStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SavingsGoalResponse(
        UUID id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        String currency,
        LocalDate targetDate,
        SavingsGoalStatus status,
        String colorToken,
        String iconName,
        BigDecimal progressPercent,
        BigDecimal remainingAmount,
        BigDecimal monthlyTarget,
        String timelineState,
        Instant completedAt,
        List<GoalContributionResponse> recentContributions
) {
}
