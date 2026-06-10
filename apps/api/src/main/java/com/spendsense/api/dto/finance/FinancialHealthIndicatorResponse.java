package com.spendsense.api.dto.finance;

import java.math.BigDecimal;

public record FinancialHealthIndicatorResponse(
        String key,
        String label,
        String state,
        BigDecimal value,
        BigDecimal benchmark,
        BigDecimal monthlyChange,
        String explanation,
        String actionHint
) {
}
