package com.spendsense.api.dto.finance;

import java.math.BigDecimal;

public record DashboardInsightSummaryResponse(
        long recurringCount,
        long subscriptionCount,
        BigDecimal subscriptionSpend,
        long spendingSpikeCount,
        BigDecimal monthOverMonthExpenseChangePercent,
        String largestExpenseChangeCategory,
        String incomeConsistencyState,
        String savingsTrendState
) {
}
