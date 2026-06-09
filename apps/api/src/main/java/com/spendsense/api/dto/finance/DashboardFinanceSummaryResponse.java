package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.util.List;

public record DashboardFinanceSummaryResponse(
        long accountCount,
        long transactionCount,
        boolean demoSeeded,
        BigDecimal totalBalance,
        BigDecimal monthSpend,
        BigDecimal monthIncome,
        BigDecimal netCashflow,
        List<AccountResponse> accounts,
        List<TransactionResponse> recentTransactions,
        List<CategorySpendResponse> categoryBreakdown,
        List<MonthlySummaryResponse> monthlySummary,
        List<ImportJobResponse> recentImports,
        BudgetOverviewResponse budgetOverview,
        List<CategorySpendResponse> topOverspendingCategories,
        List<SavingsGoalResponse> savingsGoals,
        FinancialHealthResponse financialHealth,
        SavingsMomentumResponse savingsMomentum,
        List<CategoryTrendResponse> categoryTrends
) {
}
