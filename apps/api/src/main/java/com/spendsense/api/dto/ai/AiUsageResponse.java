package com.spendsense.api.dto.ai;

import java.math.BigDecimal;

public record AiUsageResponse(
        String provider,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        BigDecimal estimatedCostMinor,
        String currency,
        Integer latencyMs
) {
}
