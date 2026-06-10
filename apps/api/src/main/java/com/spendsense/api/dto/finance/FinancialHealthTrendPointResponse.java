package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record FinancialHealthTrendPointResponse(
        Instant periodStart,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netCashflow,
        BigDecimal savingsRate,
        int score,
        String state
) {
}
