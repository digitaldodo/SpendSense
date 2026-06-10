package com.spendsense.api.dto.engagement;

public record EmailPreviewResponse(
        String templateType,
        String subject,
        String html,
        String text
) {
}
