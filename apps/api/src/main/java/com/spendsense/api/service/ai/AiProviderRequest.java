package com.spendsense.api.service.ai;

import java.util.List;

record AiProviderRequest(
        String systemPrompt,
        String userPrompt,
        String intent,
        AiFinancialContext context,
        List<String> safetyFlags
) {
}
