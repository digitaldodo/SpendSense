package com.spendsense.api.service.ai;

import com.spendsense.api.dto.finance.CategoryTrendInsightResponse;
import com.spendsense.api.dto.finance.MonthlyComparisonResponse;
import com.spendsense.api.dto.finance.RecurringPatternResponse;
import com.spendsense.api.dto.finance.SpendingAnomalyResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

record AiFinancialContext(
        Instant generatedAt,
        BigDecimal totalBalance,
        BigDecimal averageIncome,
        BigDecimal averageExpense,
        BigDecimal averageFreeCashflow,
        MonthlyComparisonResponse latestMonth,
        MonthlyComparisonResponse previousMonth,
        List<BudgetFact> budgets,
        List<GoalFact> goals,
        List<TransactionFact> recentTransactions,
        List<SpendingAnomalyResponse> anomalies,
        List<CategoryTrendInsightResponse> categoryTrends,
        List<RecurringPatternResponse> recurringPayments,
        List<String> deterministicNotes
) {
    record BudgetFact(
            String name,
            String categoryName,
            BigDecimal amount,
            BigDecimal spent,
            BigDecimal remaining,
            BigDecimal usagePercent,
            String state
    ) {
    }

    record GoalFact(
            String name,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            BigDecimal progressPercent,
            String timelineState
    ) {
    }

    record TransactionFact(
            Instant occurredAt,
            String merchantName,
            String categoryName,
            BigDecimal amount,
            String direction
    ) {
    }
}
