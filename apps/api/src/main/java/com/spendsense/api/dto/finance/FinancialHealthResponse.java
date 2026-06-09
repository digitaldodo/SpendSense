package com.spendsense.api.dto.finance;

import java.math.BigDecimal;

public record FinancialHealthResponse(
        int score,
        String state,
        BigDecimal savingsRatio,
        BigDecimal spendingConsistency,
        BigDecimal incomeExpenseStability,
        BigDecimal overspendingFrequency
) {
}
