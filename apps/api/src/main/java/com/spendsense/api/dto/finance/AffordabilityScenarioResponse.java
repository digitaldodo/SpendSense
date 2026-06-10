package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AffordabilityScenarioResponse(
        UUID scenarioId,
        Instant generatedAt,
        String state,
        String explanation,
        BigDecimal purchaseAmount,
        BigDecimal downPayment,
        BigDecimal financedAmount,
        BigDecimal monthlyEmi,
        BigDecimal totalInterest,
        BigDecimal totalPayment,
        BigDecimal safeEmiLimit,
        BigDecimal freeCashflowBefore,
        BigDecimal freeCashflowAfter,
        BigDecimal cashflowReductionPercent,
        BigDecimal savingsImpactOverTenure,
        Integer goalDelayMonths,
        String delayedGoalName,
        List<CashflowImpactPointResponse> cashflowProjection
) {
}
