package com.spendsense.api.service.delivery;

public interface EmailDeliveryProvider {
    String providerName();

    default boolean available() {
        return true;
    }

    EmailDeliveryResult send(EmailMessage message);
}
