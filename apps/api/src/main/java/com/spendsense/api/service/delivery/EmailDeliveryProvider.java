package com.spendsense.api.service.delivery;

public interface EmailDeliveryProvider {
    String providerName();

    EmailDeliveryResult send(EmailMessage message);
}
