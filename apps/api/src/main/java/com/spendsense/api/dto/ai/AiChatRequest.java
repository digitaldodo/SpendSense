package com.spendsense.api.dto.ai;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiChatRequest(
        UUID conversationId,
        @Size(max = 2200) String prompt,
        @Size(max = 64) String intent,
        UUID sourceTransactionId,
        UUID sourceBudgetId,
        UUID sourceGoalId
) {
}
