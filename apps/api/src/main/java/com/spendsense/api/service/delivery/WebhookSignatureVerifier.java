package com.spendsense.api.service.delivery;

import com.spendsense.api.config.SpendSenseProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WebhookSignatureVerifier {
    private final SpendSenseProperties properties;

    public WebhookSignatureVerifier(SpendSenseProperties properties) {
        this.properties = properties;
    }

    public boolean verify(String provider, String payload, Map<String, String> headers) {
        String secret = secretFor(provider);
        boolean requireSignature = Boolean.TRUE.equals(properties.delivery().webhooks().requireSignature());
        if (!StringUtils.hasText(secret)) {
            return !requireSignature;
        }
        String normalizedProvider = provider == null ? "" : provider.toUpperCase(Locale.ROOT);
        if ("RESEND".equals(normalizedProvider) && verifySvix(payload, headers, secret)) {
            return true;
        }
        String signature = firstHeader(headers, "x-spendsense-signature", "resend-signature", "x-signature");
        if (!StringUtils.hasText(signature)) {
            return false;
        }
        String timestamp = firstHeader(headers, "x-spendsense-timestamp", "webhook-timestamp", "svix-timestamp");
        if (StringUtils.hasText(timestamp) && !freshTimestamp(timestamp)) {
            return false;
        }
        String signedPayload = StringUtils.hasText(timestamp) ? timestamp + "." + payload : payload;
        return secureEquals(hexHmac(signedPayload, secret), signature.replace("sha256=", ""));
    }

    private boolean verifySvix(String payload, Map<String, String> headers, String secret) {
        String signatureHeader = firstHeader(headers, "svix-signature");
        String id = firstHeader(headers, "svix-id");
        String timestamp = firstHeader(headers, "svix-timestamp");
        if (!StringUtils.hasText(signatureHeader) || !StringUtils.hasText(id) || !StringUtils.hasText(timestamp)) {
            return false;
        }
        if (!freshTimestamp(timestamp)) {
            return false;
        }
        byte[] key;
        try {
            key = secret.startsWith("whsec_")
                    ? Base64.getDecoder().decode(secret.substring(6))
                    : secret.getBytes(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        String signedContent = id + "." + timestamp + "." + payload;
        String expected = base64Hmac(signedContent, key);
        List<String> signatures = List.of(signatureHeader.split(" "));
        return signatures.stream()
                .flatMap(item -> List.of(item.split(",")).stream())
                .map(String::trim)
                .filter(item -> !"v1".equals(item))
                .map(item -> item.startsWith("v1,") ? item.substring(3) : item)
                .anyMatch(item -> secureEquals(expected, item));
    }

    private String secretFor(String provider) {
        SpendSenseProperties.Webhooks webhooks = properties.delivery().webhooks();
        String normalizedProvider = provider == null ? "" : provider.toUpperCase(Locale.ROOT);
        return switch (normalizedProvider) {
            case "RESEND" -> webhooks.resendSecret();
            case "SMTP", "SMTP_FALLBACK" -> webhooks.smtpFallbackSecret();
            case "PUSH", "PUSH_PROVIDER" -> webhooks.pushProviderSecret();
            default -> "";
        };
    }

    private boolean freshTimestamp(String timestamp) {
        try {
            long epochSeconds = Long.parseLong(timestamp.trim());
            long age = Math.abs(Instant.now().getEpochSecond() - epochSeconds);
            Integer window = properties.delivery().webhooks().replayWindowSeconds();
            return age <= (window == null ? 300 : Math.max(30, window));
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String firstHeader(Map<String, String> headers, String... names) {
        for (String name : names) {
            String value = headers.get(name);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String base64Hmac(String value, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not verify webhook signature.", exception);
        }
    }

    private String hexHmac(String value, String secret) {
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not verify webhook signature.", exception);
        }
    }

    private boolean secureEquals(String expected, String actual) {
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
