package com.spendsense.api.service.delivery;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogOnlyEmailDeliveryProvider implements EmailDeliveryProvider {
    private static final Logger log = LoggerFactory.getLogger(LogOnlyEmailDeliveryProvider.class);

    @Override
    public String providerName() {
        return "LOG_ONLY";
    }

    @Override
    public EmailDeliveryResult send(EmailMessage message) {
        if (message.to() == null || message.to().isBlank()) {
            return EmailDeliveryResult.failed(providerName(), "MISSING_RECIPIENT", "No email recipient is configured.");
        }
        String messageId = UUID.nameUUIDFromBytes(
                "%s:%s:%s".formatted(message.to(), message.subject(), message.deliveryKind()).getBytes(StandardCharsets.UTF_8)
        ).toString();
        log.info(
                "email_delivery provider={} to={} kind={} subject=\"{}\" messageId={}",
                providerName(),
                message.to(),
                message.deliveryKind(),
                message.subject(),
                messageId
        );
        return EmailDeliveryResult.delivered(providerName(), messageId);
    }
}
