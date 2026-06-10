package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingAnomalyResponse(
        UUID categoryId,
        String categoryName,
        String state,
        BigDecimal currentSpend,
        BigDecimal baselineSpend,
        BigDecimal changePercent,
        BigDecimal absoluteChange,
        String message
) {
}
