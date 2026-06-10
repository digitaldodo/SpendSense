package com.spendsense.api.dto.ai;

import java.math.BigDecimal;

public record AiInsightCardResponse(
        String type,
        String state,
        String title,
        String body,
        BigDecimal primaryValue,
        BigDecimal comparisonValue,
        String actionLabel,
        String actionIntent
) {
}
