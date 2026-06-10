package com.spendsense.api.dto.finance;

import java.math.BigDecimal;

public record ReportSummaryResponse(
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netCashflow,
        BigDecimal savingsRate,
        BigDecimal recurringSpend,
        BigDecimal anomalySpend
) {
}
