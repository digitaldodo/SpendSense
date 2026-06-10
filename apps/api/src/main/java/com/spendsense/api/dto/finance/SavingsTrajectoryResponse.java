package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record SavingsTrajectoryResponse(
        Instant periodStart,
        BigDecimal netSavings,
        BigDecimal savingsRate,
        BigDecimal cumulativeSavings
) {
}
