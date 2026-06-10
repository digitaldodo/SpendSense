package com.spendsense.api.dto.ai;

import java.time.Instant;
import java.util.UUID;

public record AiFeedbackResponse(
        UUID id,
        UUID messageId,
        Integer rating,
        String feedbackType,
        Instant createdAt
) {
}
