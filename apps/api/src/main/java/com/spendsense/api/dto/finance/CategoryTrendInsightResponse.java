package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryTrendInsightResponse(
        UUID categoryId,
        String categoryName,
        String colorToken,
        BigDecimal currentSpend,
        BigDecimal previousAverage,
        BigDecimal changePercent,
        String direction,
        String state
) {
}
