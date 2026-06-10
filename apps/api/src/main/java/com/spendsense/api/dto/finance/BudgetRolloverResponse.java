package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetRolloverResponse(
        UUID budgetId,
        String budgetName,
        String categoryName,
        LocalDate sourcePeriodStart,
        LocalDate sourcePeriodEnd,
        LocalDate targetPeriodStart,
        LocalDate targetPeriodEnd,
        BigDecimal originalAmount,
        BigDecimal spentAmount,
        BigDecimal rolloverAmount,
        String state
) {
}
