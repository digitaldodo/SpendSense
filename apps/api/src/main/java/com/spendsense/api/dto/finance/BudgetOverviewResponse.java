package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.util.List;

public record BudgetOverviewResponse(
        BigDecimal totalBudgeted,
        BigDecimal totalSpent,
        BigDecimal totalRemaining,
        BigDecimal usagePercent,
        long overspentCount,
        String state,
        List<BudgetResponse> budgets
) {
}
