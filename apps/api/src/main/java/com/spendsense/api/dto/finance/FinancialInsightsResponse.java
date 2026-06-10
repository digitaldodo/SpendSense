package com.spendsense.api.dto.finance;

import java.time.Instant;
import java.util.List;

public record FinancialInsightsResponse(
        Instant generatedAt,
        Instant periodStart,
        Instant periodEnd,
        String periodLabel,
        ReportSummaryResponse summary,
        List<DeterministicInsightResponse> insights,
        List<SpendingAnomalyResponse> anomalies,
        List<RecurringPatternResponse> recurringTransactions,
        List<RecurringPatternResponse> subscriptions,
        List<MonthlyComparisonResponse> monthlyComparisons,
        List<CategoryTrendInsightResponse> categoryTrends,
        List<SavingsTrajectoryResponse> savingsTrajectory,
        IncomeStabilityResponse incomeStability,
        List<CategoryDeepDiveResponse> categoryDeepDives
) {
}
