package com.spendsense.api.dto.finance;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CategoryDeepDiveResponse(
        UUID categoryId,
        String categoryName,
        String colorToken,
        BigDecimal totalSpend,
        BigDecimal averageMonthlySpend,
        BigDecimal latestMonthSpend,
        BigDecimal trendPercent,
        List<MonthlyComparisonResponse> monthlyValues
) {
}
