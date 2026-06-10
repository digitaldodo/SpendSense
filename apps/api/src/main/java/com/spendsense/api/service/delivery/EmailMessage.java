package com.spendsense.api.service.delivery;

public record EmailMessage(
        String to,
        String subject,
        String htmlBody,
        String textBody,
        String deliveryKind
) {
}
