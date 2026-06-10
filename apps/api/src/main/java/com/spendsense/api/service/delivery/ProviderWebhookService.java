package com.spendsense.api.service.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.dto.delivery.WebhookIngestionResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProviderWebhookService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final WebhookSignatureVerifier signatureVerifier;

    public ProviderWebhookService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            WebhookSignatureVerifier signatureVerifier
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.signatureVerifier = signatureVerifier;
    }

    @Transactional
    public WebhookIngestionResponse ingest(
            String provider,
            String rawPayload,
            Map<String, String> rawHeaders,
            String sourceIp
    ) {
        String normalizedProvider = normalizeProvider(provider);
        Map<String, String> headers = lowerHeaders(rawHeaders);
        boolean signatureValid = signatureVerifier.verify(normalizedProvider, rawPayload, headers);
        Map<String, Object> payload = readPayload(rawPayload);
        NormalizedWebhookEvent event = normalizeEvent(normalizedProvider, payload);
        String payloadHash = sha256(rawPayload);
        UUID replayOf = replayOf(normalizedProvider, event.providerEventId(), payloadHash);
        boolean duplicate = replayOf != null;
        UUID webhookEventId = UUID.randomUUID();

        UUID deliveryId = null;
        boolean deliverySynced = false;
        String failureReason = signatureValid ? null : "Webhook signature could not be verified.";
        if (signatureValid && !duplicate) {
            SyncResult syncResult = syncDelivery(event);
            deliveryId = syncResult.deliveryId();
            deliverySynced = syncResult.synced();
            failureReason = syncResult.failureReason();
            if (deliverySynced) {
                recordProviderDeliveryEvent(event, deliveryId);
            }
        }

        jdbcTemplate.update("""
                insert into provider_webhook_events (
                    id, provider, channel, event_type, normalized_status, provider_event_id, provider_message_id,
                    payload_sha256, signature_valid, duplicate_event, replay_of_event_id, delivery_synced,
                    notification_delivery_id, failure_reason, source_ip, headers_json, payload_json,
                    received_at, processed_at, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """,
                webhookEventId,
                normalizedProvider,
                event.channel(),
                event.eventType(),
                event.normalizedStatus(),
                event.providerEventId(),
                event.providerMessageId(),
                payloadHash,
                signatureValid,
                duplicate,
                replayOf,
                deliverySynced,
                deliveryId,
                trim(failureReason, 720),
                trim(sourceIp, 80),
                writeJson(headers),
                rawPayload
        );

        return new WebhookIngestionResponse(
                webhookEventId,
                normalizedProvider,
                event.eventType(),
                event.normalizedStatus(),
                signatureValid,
                duplicate,
                deliverySynced,
                deliveryId
        );
    }

    private SyncResult syncDelivery(NormalizedWebhookEvent event) {
        if (!StringUtils.hasText(event.providerMessageId())) {
            return new SyncResult(null, false, "Provider message id missing.");
        }
        UUID deliveryId = jdbcTemplate.query("""
                select id from notification_deliveries
                where provider_message_id = ?
                order by created_at desc
                limit 1
                """, rs -> rs.next() ? rs.getObject("id", UUID.class) : null, event.providerMessageId());
        if (deliveryId == null) {
            return new SyncResult(null, false, "No delivery matched provider message id.");
        }
        if ("DELIVERED".equals(event.normalizedStatus())) {
            jdbcTemplate.update("""
                    update notification_deliveries
                    set status = 'DELIVERED', delivered_at = coalesce(delivered_at, current_timestamp),
                        failed_at = null, next_retry_at = null, error_code = null, error_message = null,
                        updated_at = current_timestamp
                    where id = ?
                    """, deliveryId);
            return new SyncResult(deliveryId, true, null);
        }
        if ("FAILED".equals(event.normalizedStatus())) {
            jdbcTemplate.update("""
                    update notification_deliveries
                    set status = 'FAILED', failed_at = coalesce(failed_at, current_timestamp),
                        error_code = ?, error_message = ?, updated_at = current_timestamp
                    where id = ? and status <> 'DELIVERED'
                    """, "WEBHOOK_" + event.eventType(), trim("Provider webhook reported " + event.eventType(), 520), deliveryId);
            return new SyncResult(deliveryId, true, null);
        }
        if ("RETRY_SCHEDULED".equals(event.normalizedStatus())) {
            jdbcTemplate.update("""
                    update notification_deliveries
                    set status = 'RETRY_SCHEDULED', error_code = ?, error_message = ?,
                        next_retry_at = coalesce(next_retry_at, current_timestamp + interval '5 minutes'),
                        updated_at = current_timestamp
                    where id = ? and status not in ('DELIVERED', 'FAILED')
                    """, "WEBHOOK_" + event.eventType(), trim("Provider webhook reported " + event.eventType(), 520), deliveryId);
            return new SyncResult(deliveryId, true, null);
        }
        return new SyncResult(deliveryId, false, "Event did not represent a terminal delivery state.");
    }

    private void recordProviderDeliveryEvent(NormalizedWebhookEvent event, UUID deliveryId) {
        jdbcTemplate.update("""
                insert into provider_delivery_events (
                    id, notification_delivery_id, provider, channel, event_type, status, provider_message_id,
                    latency_ms, error_code, error_message, metadata_json, observed_at, created_at
                ) values (?, ?, ?, ?, ?, ?, ?, null, ?, ?, ?, current_timestamp, current_timestamp)
                """,
                UUID.randomUUID(),
                deliveryId,
                event.provider(),
                event.channel(),
                "WEBHOOK_" + event.eventType(),
                event.normalizedStatus(),
                event.providerMessageId(),
                "FAILED".equals(event.normalizedStatus()) ? "WEBHOOK_" + event.eventType() : null,
                "FAILED".equals(event.normalizedStatus()) ? trim("Provider webhook reported " + event.eventType(), 720) : null,
                writeJson(Map.of("providerEventId", nullToEmpty(event.providerEventId())))
        );
    }

    private UUID replayOf(String provider, String providerEventId, String payloadHash) {
        if (StringUtils.hasText(providerEventId)) {
            UUID replay = jdbcTemplate.query("""
                    select id from provider_webhook_events
                    where provider = ? and provider_event_id = ?
                    order by received_at asc
                    limit 1
                    """, rs -> rs.next() ? rs.getObject("id", UUID.class) : null, provider, providerEventId);
            if (replay != null) {
                return replay;
            }
        }
        return jdbcTemplate.query("""
                select id from provider_webhook_events
                where provider = ? and payload_sha256 = ?
                order by received_at asc
                limit 1
                """, rs -> rs.next() ? rs.getObject("id", UUID.class) : null, provider, payloadHash);
    }

    private NormalizedWebhookEvent normalizeEvent(String provider, Map<String, Object> payload) {
        String eventType = firstText(payload, "type", "event_type", "eventType", "name");
        Map<String, Object> data = objectMap(payload.get("data"));
        if (!StringUtils.hasText(eventType)) {
            eventType = firstText(data, "type", "event_type", "eventType", "status");
        }
        eventType = normalizeEventType(eventType);
        String channel = firstText(payload, "channel");
        if (!StringUtils.hasText(channel)) {
            channel = firstText(data, "channel");
        }
        if (!StringUtils.hasText(channel)) {
            channel = "EMAIL";
        }
        String providerEventId = firstText(payload, "id", "event_id", "eventId");
        String providerMessageId = firstText(payload, "provider_message_id", "message_id", "email_id", "emailId");
        if (!StringUtils.hasText(providerMessageId)) {
            providerMessageId = firstText(data, "email_id", "emailId", "message_id", "messageId", "id");
        }
        String status = firstText(payload, "status");
        if (!StringUtils.hasText(status)) {
            status = firstText(data, "status", "state");
        }
        return new NormalizedWebhookEvent(
                provider,
                channel.toUpperCase(Locale.ROOT),
                eventType,
                normalizeStatus(eventType, status),
                providerEventId,
                providerMessageId
        );
    }

    private String normalizeStatus(String eventType, String status) {
        String value = StringUtils.hasText(status) ? status : eventType;
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("delivered")) {
            return "DELIVERED";
        }
        if (normalized.contains("bounce") || normalized.contains("complain")
                || normalized.contains("fail") || normalized.contains("reject")) {
            return "FAILED";
        }
        if (normalized.contains("delay") || normalized.contains("defer")) {
            return "RETRY_SCHEDULED";
        }
        return "OBSERVED";
    }

    private String normalizeEventType(String value) {
        if (!StringUtils.hasText(value)) {
            return "UNKNOWN";
        }
        return value.trim().replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private String firstText(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null && StringUtils.hasText(value.toString())) {
                return value.toString();
            }
        }
        return null;
    }

    private Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private Map<String, Object> readPayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return Map.of("raw", payload);
        }
    }

    private Map<String, String> lowerHeaders(Map<String, String> headers) {
        Map<String, String> result = new LinkedHashMap<>();
        headers.forEach((key, value) -> result.put(key.toLowerCase(Locale.ROOT), value));
        return result;
    }

    private String normalizeProvider(String provider) {
        return StringUtils.hasText(provider) ? provider.trim().toUpperCase(Locale.ROOT) : "UNKNOWN";
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash webhook payload.", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write webhook JSON.", exception);
        }
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record NormalizedWebhookEvent(
            String provider,
            String channel,
            String eventType,
            String normalizedStatus,
            String providerEventId,
            String providerMessageId
    ) {
    }

    private record SyncResult(UUID deliveryId, boolean synced, String failureReason) {
    }
}
