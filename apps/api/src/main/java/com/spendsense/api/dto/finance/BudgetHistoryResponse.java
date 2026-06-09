package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetHistoryResponse(
        UUID id,
        UUID budgetId,
        String budgetName,
        String categoryName,
        String action,
        BigDecimal previousAmount,
        BigDecimal newAmount,
        String previousName,
        String newName,
        LocalDate periodStart,
        LocalDate periodEnd,
        String reason,
        Instant createdAt
) {
}
