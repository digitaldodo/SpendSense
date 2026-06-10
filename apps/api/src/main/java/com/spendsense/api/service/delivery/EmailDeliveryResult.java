package com.spendsense.api.service.delivery;

public record EmailDeliveryResult(
        boolean delivered,
        String provider,
        String providerMessageId,
        String errorCode,
        String errorMessage
) {
    public static EmailDeliveryResult delivered(String provider, String providerMessageId) {
        return new EmailDeliveryResult(true, provider, providerMessageId, null, null);
    }

    public static EmailDeliveryResult failed(String provider, String errorCode, String errorMessage) {
        return new EmailDeliveryResult(false, provider, null, errorCode, errorMessage);
    }
}
