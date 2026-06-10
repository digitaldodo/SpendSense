package com.spendsense.api.service.ai;

import java.util.List;

record AiSafetyResult(
        String sanitizedPrompt,
        String intent,
        boolean blocked,
        String safetyLevel,
        List<String> flags
) {
}
