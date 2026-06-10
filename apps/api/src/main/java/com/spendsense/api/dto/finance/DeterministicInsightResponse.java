package com.spendsense.api.dto.finance;

import java.math.BigDecimal;

public record DeterministicInsightResponse(
        String type,
        String state,
        String title,
        String body,
        BigDecimal primaryValue,
        BigDecimal comparisonValue,
        String actionLabel
) {
}
