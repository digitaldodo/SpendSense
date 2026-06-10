package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record MonthlyComparisonResponse(
        Instant periodStart,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netCashflow,
        BigDecimal incomeChange,
        BigDecimal expenseChange,
        BigDecimal expenseChangePercent,
        BigDecimal savingsRate
) {
}
