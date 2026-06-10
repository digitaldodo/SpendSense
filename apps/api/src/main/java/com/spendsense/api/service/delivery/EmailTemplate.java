package com.spendsense.api.service.delivery;

public record EmailTemplate(
        String templateType,
        String subject,
        String html,
        String text
) {
}
