package com.spendsense.api.dto.finance;

import java.math.BigDecimal;

public record IncomeStabilityResponse(
        String state,
        BigDecimal averageIncome,
        BigDecimal averageDeviation,
        BigDecimal stabilityScore,
        int monthsReviewed
) {
}
