package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record MonthlySummaryResponse(
        Instant periodStart,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netCashflow
) {
}
