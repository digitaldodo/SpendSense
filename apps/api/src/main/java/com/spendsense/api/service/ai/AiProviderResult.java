package com.spendsense.api.service.ai;

import com.spendsense.api.dto.ai.AiInsightCardResponse;
import java.math.BigDecimal;
import java.util.List;

record AiProviderResult(
        String content,
        List<AiInsightCardResponse> insightCards,
        List<String> followUpPrompts,
        List<String> citations,
        String provider,
        String model,
        int promptTokens,
        int completionTokens,
        int latencyMs,
        BigDecimal estimatedCostMinor
) {
}
