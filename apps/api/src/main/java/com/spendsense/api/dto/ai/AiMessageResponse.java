package com.spendsense.api.dto.ai;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiMessageResponse(
        UUID id,
        UUID conversationId,
        String role,
        String intent,
        String content,
        List<AiInsightCardResponse> insightCards,
        List<String> followUpPrompts,
        List<String> safetyFlags,
        String provider,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        Integer latencyMs,
        Instant createdAt
) {
}
