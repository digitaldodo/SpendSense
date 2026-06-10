package com.spendsense.api.service.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.dto.engagement.DeliveryHistoryResponse;
import com.spendsense.api.dto.engagement.DeliveryRetryResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.user.UserProfileSyncService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDeliveryService {
    private static final int MAX_ATTEMPTS = 3;
    private final UserProfileSyncService userProfileSyncService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final EmailDeliveryProvider emailDeliveryProvider;
    private final Clock clock;

    public NotificationDeliveryService(
            UserProfileSyncService userProfileSyncService,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            EmailDeliveryProvider emailDeliveryProvider
    ) {
        this.userProfileSyncService = userProfileSyncService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.emailDeliveryProvider = emailDeliveryProvider;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public UUID queueEmail(
            UUID userProfileId,
            UUID notificationId,
            UUID scheduledReportId,
            UUID generatedReportId,
            String deliveryKind,
            EmailTemplate template,
            String traceId
    ) {
        Recipient recipient = recipient(userProfileId);
        UUID deliveryId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into notification_deliveries (
                    id, notification_id, scheduled_report_id, generated_report_id, user_profile_id,
                    delivery_kind, channel, provider, recipient, subject, status, trace_id, payload_json,
                    created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, 'EMAIL', ?, ?, ?, 'PENDING', ?, ?, current_timestamp, current_timestamp)
                """,
                deliveryId,
                notificationId,
                scheduledReportId,
                generatedReportId,
                userProfileId,
                deliveryKind,
                emailDeliveryProvider.providerName(),
                recipient.email(),
                template.subject(),
                traceId,
                writeJson(Map.of("html", template.html(), "text", template.text(), "templateType", template.templateType()))
        );
        return deliveryId;
    }

    @Transactional
    public DeliveryHistoryResponse retryNow(SupabasePrincipal principal, UUID deliveryId) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        jdbcTemplate.update("""
                update notification_deliveries
                set status = 'RETRY_SCHEDULED', next_retry_at = current_timestamp, updated_at = current_timestamp
                where id = ? and user_profile_id = ? and status in ('FAILED', 'RETRY_SCHEDULED')
                """, deliveryId, userProfileId);
        attemptDelivery(deliveryId);
        return delivery(deliveryId, userProfileId);
    }

    @Transactional
    public int runPendingRetries(int limit) {
        List<UUID> deliveryIds = jdbcTemplate.query("""
                select id from notification_deliveries
                where status in ('PENDING', 'RETRY_SCHEDULED')
                  and (next_retry_at is null or next_retry_at <= current_timestamp)
                order by coalesce(next_retry_at, created_at) asc
                limit ?
                """, (rs, rowNum) -> rs.getObject("id", UUID.class), limit);
        deliveryIds.forEach(this::attemptDelivery);
        return deliveryIds.size();
    }

    @Transactional
    public int dispatchNotificationEmailCandidates(int limit) {
        List<NotificationCandidate> candidates = jdbcTemplate.query("""
                select n.id, n.user_profile_id, n.notification_type, n.title, n.body
                from notifications n
                join notification_preferences p on p.user_profile_id = n.user_profile_id
                where n.lifecycle_status = 'ACTIVE'
                  and p.email_enabled = true
                  and (
                    (n.notification_type in ('BUDGET_NEARING_LIMIT', 'BUDGET_EXCEEDED') and p.budget_alert_email_enabled = true)
                    or (n.notification_type = 'RECURRING_PAYMENT_DUE' and p.recurring_reminder_email_enabled = true)
                    or (n.notification_type in ('REPORT_READY', 'WEEKLY_SUMMARY_READY') and p.report_email_enabled = true)
                  )
                  and not exists (
                    select 1 from notification_deliveries d
                    where d.notification_id = n.id and d.channel = 'EMAIL'
                  )
                order by n.priority asc, n.created_at asc
                limit ?
                """, this::notificationCandidateRow, limit);
        for (NotificationCandidate candidate : candidates) {
            EmailTemplate template = notificationTemplate(candidate);
            queueEmail(candidate.userProfileId(), candidate.id(), null, null, candidate.notificationType(), template, null);
        }
        return candidates.size();
    }

    public List<DeliveryHistoryResponse> history(SupabasePrincipal principal) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return jdbcTemplate.query("""
                select * from notification_deliveries
                where user_profile_id = ?
                order by created_at desc
                limit 80
                """, this::deliveryRow, userProfileId);
    }

    public List<DeliveryRetryResponse> retries(SupabasePrincipal principal, UUID deliveryId) {
        UUID userProfileId = userProfileSyncService.syncAuthenticatedUser(principal).getId();
        return jdbcTemplate.query("""
                select r.* from delivery_retries r
                join notification_deliveries d on d.id = r.notification_delivery_id
                where d.user_profile_id = ? and d.id = ?
                order by r.attempt_number desc
                """, this::retryRow, userProfileId, deliveryId);
    }

    private void attemptDelivery(UUID deliveryId) {
        DeliveryAttempt delivery = jdbcTemplate.queryForObject(
                "select * from notification_deliveries where id = ?",
                this::deliveryAttemptRow,
                deliveryId
        );
        if (delivery == null || delivery.attemptCount() >= MAX_ATTEMPTS || "DELIVERED".equals(delivery.status())) {
            return;
        }
        int attemptNumber = delivery.attemptCount() + 1;
        UUID retryId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into delivery_retries (
                    id, notification_delivery_id, attempt_number, scheduled_for, status, created_at, updated_at
                ) values (?, ?, ?, current_timestamp, 'RUNNING', current_timestamp, current_timestamp)
                """, retryId, deliveryId, attemptNumber);
        EmailDeliveryResult result = emailDeliveryProvider.send(new EmailMessage(
                delivery.recipient(),
                delivery.subject(),
                delivery.html(),
                delivery.text(),
                delivery.deliveryKind()
        ));
        if (result.delivered()) {
            jdbcTemplate.update("""
                    update notification_deliveries
                    set status = 'DELIVERED', attempt_count = ?, last_attempt_at = current_timestamp,
                        delivered_at = current_timestamp, failed_at = null, next_retry_at = null,
                        provider_message_id = ?, error_code = null, error_message = null, updated_at = current_timestamp
                    where id = ?
                    """, attemptNumber, result.providerMessageId(), deliveryId);
            jdbcTemplate.update("""
                    update delivery_retries
                    set status = 'DELIVERED', attempted_at = current_timestamp, updated_at = current_timestamp
                    where id = ?
                    """, retryId);
            return;
        }
        boolean terminal = attemptNumber >= MAX_ATTEMPTS;
        Instant nextRetryAt = terminal ? null : Instant.now(clock).plusSeconds(300L * attemptNumber);
        jdbcTemplate.update("""
                update notification_deliveries
                set status = ?, attempt_count = ?, last_attempt_at = current_timestamp,
                    failed_at = current_timestamp, next_retry_at = ?,
                    error_code = ?, error_message = ?, updated_at = current_timestamp
                where id = ?
                """,
                terminal ? "FAILED" : "RETRY_SCHEDULED",
                attemptNumber,
                nextRetryAt,
                result.errorCode(),
                trim(result.errorMessage(), 520),
                deliveryId
        );
        jdbcTemplate.update("""
                update delivery_retries
                set status = ?, attempted_at = current_timestamp, error_code = ?, error_message = ?, updated_at = current_timestamp
                where id = ?
                """, terminal ? "FAILED" : "RETRY_SCHEDULED", result.errorCode(), trim(result.errorMessage(), 520), retryId);
    }

    private DeliveryHistoryResponse delivery(UUID deliveryId, UUID userProfileId) {
        return jdbcTemplate.queryForObject(
                "select * from notification_deliveries where id = ? and user_profile_id = ?",
                this::deliveryRow,
                deliveryId,
                userProfileId
        );
    }

    private Recipient recipient(UUID userProfileId) {
        return jdbcTemplate.queryForObject("""
                select coalesce(nullif(p.email_address, ''), u.email) as email
                from user_profiles u
                left join notification_preferences p on p.user_profile_id = u.id
                where u.id = ?
                """, (rs, rowNum) -> new Recipient(rs.getString("email")), userProfileId);
    }

    private EmailTemplate notificationTemplate(NotificationCandidate candidate) {
        String subject = switch (candidate.notificationType()) {
            case "BUDGET_NEARING_LIMIT", "BUDGET_EXCEEDED" -> "SpendSense budget alert";
            case "RECURRING_PAYMENT_DUE" -> "Upcoming recurring payment";
            default -> "SpendSense report update";
        };
        String html = """
                <!doctype html><html><body style="margin:0;background:#f5f7f8;font-family:Arial,Helvetica,sans-serif;color:#17201b">
                <div style="max-width:620px;margin:0 auto;padding:24px 14px">
                <div style="background:#fff;border:1px solid #dfe7e2;border-radius:8px;padding:24px">
                <div style="color:#23614b;font-weight:700;font-size:14px">SpendSense</div>
                <h1 style="font-size:22px;line-height:1.3;margin:12px 0 8px">%s</h1>
                <p style="font-size:15px;line-height:1.6;color:#52635a;margin:0">%s</p>
                </div></div></body></html>
                """.formatted(escape(candidate.title()), escape(candidate.body()));
        String text = "SpendSense\n%s\n%s".formatted(candidate.title(), candidate.body());
        return new EmailTemplate(candidate.notificationType(), subject, html, text);
    }

    private DeliveryAttempt deliveryAttemptRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, String> payload = readPayload(rs.getString("payload_json"));
        return new DeliveryAttempt(
                rs.getObject("id", UUID.class),
                rs.getString("delivery_kind"),
                rs.getString("status"),
                rs.getString("recipient"),
                rs.getString("subject"),
                rs.getInt("attempt_count"),
                payload.getOrDefault("html", ""),
                payload.getOrDefault("text", "")
        );
    }

    private DeliveryHistoryResponse deliveryRow(ResultSet rs, int rowNum) throws SQLException {
        return new DeliveryHistoryResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("notification_id", UUID.class),
                rs.getObject("scheduled_report_id", UUID.class),
                rs.getObject("generated_report_id", UUID.class),
                rs.getString("delivery_kind"),
                rs.getString("channel"),
                rs.getString("provider"),
                rs.getString("recipient"),
                rs.getString("subject"),
                rs.getString("status"),
                rs.getInt("attempt_count"),
                instant(rs, "next_retry_at"),
                instant(rs, "last_attempt_at"),
                instant(rs, "delivered_at"),
                instant(rs, "failed_at"),
                rs.getString("error_code"),
                rs.getString("error_message"),
                instant(rs, "created_at")
        );
    }

    private DeliveryRetryResponse retryRow(ResultSet rs, int rowNum) throws SQLException {
        return new DeliveryRetryResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("notification_delivery_id", UUID.class),
                rs.getInt("attempt_number"),
                instant(rs, "scheduled_for"),
                instant(rs, "attempted_at"),
                rs.getString("status"),
                rs.getString("error_code"),
                rs.getString("error_message")
        );
    }

    private NotificationCandidate notificationCandidateRow(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationCandidate(
                rs.getObject("id", UUID.class),
                rs.getObject("user_profile_id", UUID.class),
                rs.getString("notification_type"),
                rs.getString("title"),
                rs.getString("body")
        );
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Map<String, String> readPayload(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write delivery payload.", exception);
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private record Recipient(String email) {
    }

    private record NotificationCandidate(UUID id, UUID userProfileId, String notificationType, String title, String body) {
    }

    private record DeliveryAttempt(
            UUID id,
            String deliveryKind,
            String status,
            String recipient,
            String subject,
            int attemptCount,
            String html,
            String text
    ) {
    }
}
