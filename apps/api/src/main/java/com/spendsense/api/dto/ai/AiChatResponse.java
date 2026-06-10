package com.spendsense.api.dto.ai;

import java.util.List;

public record AiChatResponse(
        AiConversationSummaryResponse conversation,
        AiMessageResponse userMessage,
        AiMessageResponse assistantMessage,
        List<AiInsightCardResponse> insightCards,
        List<String> followUpPrompts,
        AiUsageResponse usage,
        boolean grounded,
        String safetyLevel,
        List<String> citations
) {
}
