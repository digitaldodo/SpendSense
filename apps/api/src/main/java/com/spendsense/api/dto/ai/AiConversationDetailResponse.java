package com.spendsense.api.dto.ai;

import java.util.List;

public record AiConversationDetailResponse(
        AiConversationSummaryResponse conversation,
        List<AiMessageResponse> messages
) {
}
