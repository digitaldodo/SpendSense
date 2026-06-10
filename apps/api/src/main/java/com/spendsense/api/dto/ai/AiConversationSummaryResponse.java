package com.spendsense.api.dto.ai;

import java.time.Instant;
import java.util.UUID;

public record AiConversationSummaryResponse(
        UUID id,
        String title,
        String status,
        String contextScope,
        Instant lastMessageAt,
        Instant createdAt,
        String lastMessagePreview
) {
}
