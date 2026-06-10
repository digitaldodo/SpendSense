package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectionResponse(
        UUID projectionId,
        Instant generatedAt,
        String state,
        BigDecimal currentBalance,
        BigDecimal monthlySavings,
        BigDecimal averageMonthlyExpense,
        BigDecimal emergencyRunwayMonths,
        BigDecimal fireStyleTarget,
        Integer monthsToFireStyleTarget,
        List<ProjectionPointResponse> trajectory,
        List<String> notes
) {
}
