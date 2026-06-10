package com.spendsense.api.service.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.config.SpendSenseProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ResendEmailDeliveryProvider implements EmailDeliveryProvider {
    private final SpendSenseProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ResendEmailDeliveryProvider(SpendSenseProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }

    @Override
    public String providerName() {
        return "RESEND";
    }

    @Override
    public boolean available() {
        SpendSenseProperties.Resend resend = properties.delivery().email().resend();
        return Boolean.TRUE.equals(resend.enabled()) && StringUtils.hasText(resend.apiKey());
    }

    @Override
    public EmailDeliveryResult send(EmailMessage message) {
        if (!StringUtils.hasText(message.to())) {
            return EmailDeliveryResult.failed(providerName(), "MISSING_RECIPIENT", "No email recipient is configured.");
        }
        if (!available()) {
            return EmailDeliveryResult.failed(providerName(), "PROVIDER_DISABLED", "Resend is not enabled.");
        }

        SpendSenseProperties.Resend resend = properties.delivery().email().resend();
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "from", "%s <%s>".formatted(resend.fromName(), resend.fromEmail()),
                    "to", new String[]{message.to()},
                    "subject", message.subject(),
                    "html", message.htmlBody(),
                    "text", message.textBody()
            ));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + resend.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String providerId = readProviderId(response.body());
                return EmailDeliveryResult.delivered(providerName(), providerId);
            }
            return EmailDeliveryResult.failed(
                    providerName(),
                    "RESEND_%s".formatted(response.statusCode()),
                    trim(response.body(), 520)
            );
        } catch (IOException exception) {
            return EmailDeliveryResult.failed(providerName(), "RESEND_IO_ERROR", trim(exception.getMessage(), 520));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return EmailDeliveryResult.failed(providerName(), "RESEND_INTERRUPTED", "Resend request was interrupted.");
        }
    }

    private String readProviderId(String body) {
        try {
            Map<?, ?> payload = objectMapper.readValue(body, Map.class);
            Object id = payload.get("id");
            return id == null ? null : id.toString();
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
