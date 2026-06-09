package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        String name,
        CategoryResponse category,
        BigDecimal amount,
        String currency,
        LocalDate periodStart,
        LocalDate periodEnd,
        boolean rolloverEnabled,
        boolean active,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal usagePercent,
        String state
) {
}
