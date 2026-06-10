package com.spendsense.api.service.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.dto.admin.AlertAcknowledgmentRequest;
import com.spendsense.api.dto.admin.DeliveryTimelineEventResponse;
import com.spendsense.api.dto.admin.IncidentLogResponse;
import com.spendsense.api.dto.admin.OperationalAlertResponse;
import com.spendsense.api.dto.admin.ProviderWebhookEventResponse;
import com.spendsense.api.dto.admin.ReliabilityOverviewResponse;
import com.spendsense.api.dto.admin.RunbookEntryResponse;
import com.spendsense.api.security.SupabasePrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OperationalReliabilityService {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AdminAuditService adminAuditService;

    public OperationalReliabilityService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            AdminAuditService adminAuditService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.adminAuditService = adminAuditService;
    }

    @Transactional
    public ReliabilityOverviewResponse overview() {
        refreshReliabilityState();
        List<OperationalAlertResponse> alerts = alerts("ACTIVE", null, 12);
        List<IncidentLogResponse> incidents = incidents("OPEN", 8);
        List<ProviderWebhookEventResponse> webhooks = webhookEvents(null, null, 8);
        List<RunbookEntryResponse> runbooks = runbooks(null, null, null, 6);
        String status = alerts.stream().anyMatch(alert -> "CRITICAL".equals(alert.severity()))
                ? "INCIDENT"
                : alerts.isEmpty() ? "OK" : "DEGRADED";
        return new ReliabilityOverviewResponse(status, Instant.now(), alerts, incidents, webhooks, runbooks);
    }

    @Transactional
    public List<OperationalAlertResponse> alerts(String status, String severity, int limit) {
        refreshReliabilityState();
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select * from operational_alerts where 1 = 1");
        if (StringUtils.hasText(status)) {
            sql.append(" and status = ?");
            params.add(status);
        }
        if (StringUtils.hasText(severity)) {
            sql.append(" and severity = ?");
            params.add(severity);
        }
        sql.append(" order by case severity when 'CRITICAL' then 0 when 'WARNING' then 1 else 2 end, last_seen_at desc limit ?");
        params.add(Math.min(Math.max(limit, 1), 100));
        return jdbcTemplate.query(sql.toString(), this::alertRow, params.toArray());
    }

    @Transactional
    public OperationalAlertResponse acknowledgeAlert(
            SupabasePrincipal principal,
            UUID alertId,
            AlertAcknowledgmentRequest request,
            String traceId
    ) {
        jdbcTemplate.update("""
                update operational_alerts
                set status = 'ACKNOWLEDGED', acknowledged_at = current_timestamp, acknowledged_by = ?,
                    acknowledged_by_email = ?, acknowledgment_note = ?, updated_at = current_timestamp
                where id = ? and status = 'ACTIVE'
                """,
                principal.id(),
                principal.email(),
                trim(request == null ? null : request.note(), 520),
                alertId
        );
        adminAuditService.record(
                principal,
                "OPERATIONAL_ALERT_ACKNOWLEDGED",
                "operational_alert",
                alertId,
                request == null ? null : request.note(),
                Map.of(),
                traceId
        );
        return jdbcTemplate.queryForObject("select * from operational_alerts where id = ?", this::alertRow, alertId);
    }

    @Transactional
    public List<IncidentLogResponse> incidents(String status, int limit) {
        refreshReliabilityState();
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select * from incident_logs where 1 = 1");
        if (StringUtils.hasText(status)) {
            sql.append(" and status = ?");
            params.add(status);
        }
        sql.append(" order by case severity when 'CRITICAL' then 0 when 'WARNING' then 1 else 2 end, last_event_at desc limit ?");
        params.add(Math.min(Math.max(limit, 1), 100));
        return jdbcTemplate.query(sql.toString(), this::incidentRow, params.toArray());
    }

    public IncidentLogResponse incident(UUID incidentId) {
        return jdbcTemplate.queryForObject("select * from incident_logs where id = ?", this::incidentRow, incidentId);
    }

    public List<RunbookEntryResponse> runbooks(String search, String severity, String category, int limit) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select * from runbook_entries where active = true");
        if (StringUtils.hasText(search)) {
            sql.append(" and (title ilike ? or summary ilike ? or symptoms ilike ? or diagnosis_steps ilike ? or mitigation_steps ilike ?)");
            String like = "%" + search + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (StringUtils.hasText(severity)) {
            sql.append(" and severity = ?");
            params.add(severity);
        }
        if (StringUtils.hasText(category)) {
            sql.append(" and category = ?");
            params.add(category);
        }
        sql.append(" order by case severity when 'CRITICAL' then 0 when 'WARNING' then 1 else 2 end, title asc limit ?");
        params.add(Math.min(Math.max(limit, 1), 100));
        return jdbcTemplate.query(sql.toString(), this::runbookRow, params.toArray());
    }

    public RunbookEntryResponse runbook(String slug) {
        return jdbcTemplate.queryForObject("select * from runbook_entries where slug = ? and active = true", this::runbookRow, slug);
    }

    public List<ProviderWebhookEventResponse> webhookEvents(String provider, String status, int limit) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select * from provider_webhook_events where 1 = 1");
        if (StringUtils.hasText(provider)) {
            sql.append(" and provider = ?");
            params.add(provider);
        }
        if (StringUtils.hasText(status)) {
            sql.append(" and normalized_status = ?");
            params.add(status);
        }
        sql.append(" order by received_at desc limit ?");
        params.add(Math.min(Math.max(limit, 1), 200));
        return jdbcTemplate.query(sql.toString(), this::webhookRow, params.toArray());
    }

    public List<DeliveryTimelineEventResponse> deliveryTimeline(UUID deliveryId) {
        return jdbcTemplate.query("""
                select id, notification_delivery_id, 'provider_event' as source, provider, event_type, status,
                       coalesce(error_message, provider_message_id, event_type) as message, observed_at
                from provider_delivery_events
                where notification_delivery_id = ?
                union all
                select id, notification_delivery_id, 'webhook' as source, provider, event_type, normalized_status as status,
                       coalesce(failure_reason, provider_message_id, event_type) as message, received_at as observed_at
                from provider_webhook_events
                where notification_delivery_id = ?
                union all
                select id, notification_delivery_id, 'retry' as source, 'INTERNAL' as provider, 'RETRY_' || status as event_type,
                       status, coalesce(error_message, 'Retry attempt ' || attempt_number::text) as message, coalesce(attempted_at, scheduled_for) as observed_at
                from delivery_retries
                where notification_delivery_id = ?
                order by observed_at desc
                """, this::timelineRow, deliveryId, deliveryId, deliveryId);
    }

    private void refreshReliabilityState() {
        List<String> activeKeys = new ArrayList<>();
        evaluateQueueRules(activeKeys);
        evaluateWorkerRules(activeKeys);
        evaluateProviderRules(activeKeys);
        evaluateWebhookRules(activeKeys);
        evaluateReportRules(activeKeys);
        resolveClearedAlerts(activeKeys);
        aggregateIncidents();
    }

    private void evaluateQueueRules(List<String> activeKeys) {
        jdbcTemplate.query("""
                select queue_name,
                       count(*) filter (where status = 'DEAD_LETTER') as dead_letter,
                       coalesce(cast(extract(epoch from (current_timestamp - min(scheduled_for) filter (
                         where status in ('PENDING', 'RETRY_SCHEDULED') and scheduled_for <= current_timestamp
                       ))) as bigint), 0) as lag_seconds
                from worker_queues
                group by queue_name
                """, rs -> {
            String queueName = rs.getString("queue_name");
            long deadLetter = rs.getLong("dead_letter");
            long lagSeconds = rs.getLong("lag_seconds");
            if (deadLetter > 0) {
                upsertAlert(
                        activeKeys,
                        "retry-exhausted:" + queueName,
                        "CRITICAL",
                        "Retry exhaustion in " + queueName,
                        "%d job(s) are in the dead-letter queue and need operator review.".formatted(deadLetter),
                        "worker_queue",
                        queueName,
                        "retry-exhaustion",
                        Map.of("deadLetter", deadLetter)
                );
            }
            if (lagSeconds > 900) {
                upsertAlert(
                        activeKeys,
                        "queue-backlog:" + queueName,
                        lagSeconds > 3600 ? "CRITICAL" : "WARNING",
                        "Queue backlog on " + queueName,
                        "Oldest ready job is waiting %d seconds.".formatted(lagSeconds),
                        "worker_queue",
                        queueName,
                        "queue-backlog",
                        Map.of("lagSeconds", lagSeconds)
                );
            }
        });
    }

    private void evaluateWorkerRules(List<String> activeKeys) {
        Instant latestHeartbeat = jdbcTemplate.query("""
                select heartbeat_at from worker_job_logs order by heartbeat_at desc limit 1
                """, rs -> rs.next() ? instant(rs, "heartbeat_at") : null);
        if (latestHeartbeat == null || latestHeartbeat.isBefore(Instant.now().minusSeconds(1800))) {
            upsertAlert(
                    activeKeys,
                    "stale-worker:heartbeat",
                    latestHeartbeat == null || latestHeartbeat.isBefore(Instant.now().minusSeconds(3600)) ? "CRITICAL" : "WARNING",
                    "Worker heartbeat is stale",
                    latestHeartbeat == null ? "No worker heartbeat has been recorded." : "Latest worker heartbeat is older than 30 minutes.",
                    "worker_heartbeat",
                    null,
                    "stuck-workers",
                    Map.of("latestHeartbeat", latestHeartbeat == null ? "" : latestHeartbeat.toString())
            );
        }
        Long expiredLocks = jdbcTemplate.queryForObject("""
                select count(*) from worker_queues
                where status = 'RUNNING' and locked_until < current_timestamp
                """, Long.class);
        if (expiredLocks != null && expiredLocks > 0) {
            upsertAlert(
                    activeKeys,
                    "worker-lock-expired",
                    "WARNING",
                    "Worker locks expired",
                    "%d running queue job(s) have expired locks.".formatted(expiredLocks),
                    "worker_queue",
                    null,
                    "stuck-workers",
                    Map.of("expiredLocks", expiredLocks)
            );
        }
    }

    private void evaluateProviderRules(List<String> activeKeys) {
        jdbcTemplate.query("""
                select provider, channel, count(*) as attempts,
                       count(*) filter (where status <> 'DELIVERED') as failures
                from provider_delivery_events
                where observed_at >= current_timestamp - interval '1 hour'
                group by provider, channel
                having count(*) >= 3 and count(*) filter (where status <> 'DELIVERED') >= 3
                """, rs -> {
            String provider = rs.getString("provider");
            long attempts = rs.getLong("attempts");
            long failures = rs.getLong("failures");
            double rate = attempts == 0 ? 0 : (double) failures / attempts;
            upsertAlert(
                    activeKeys,
                    "provider-outage:" + provider,
                    rate >= 0.8 ? "CRITICAL" : "WARNING",
                    "Provider outage signal for " + provider,
                    "%d of %d recent provider events failed.".formatted(failures, attempts),
                    "provider",
                    provider,
                    "provider-outage",
                    Map.of("attempts", attempts, "failures", failures)
            );
        });
    }

    private void evaluateWebhookRules(List<String> activeKeys) {
        Long invalidSignatures = jdbcTemplate.queryForObject("""
                select count(*) from provider_webhook_events
                where signature_valid = false and received_at >= current_timestamp - interval '1 hour'
                """, Long.class);
        if (invalidSignatures != null && invalidSignatures >= 3) {
            upsertAlert(
                    activeKeys,
                    "webhook-invalid-signature",
                    "WARNING",
                    "Webhook signatures are failing",
                    "%d webhook request(s) had invalid signatures in the last hour.".formatted(invalidSignatures),
                    "provider_webhook",
                    null,
                    "webhook-failures",
                    Map.of("invalidSignatures", invalidSignatures)
            );
        }
        Long unsyncedFailures = jdbcTemplate.queryForObject("""
                select count(*) from provider_webhook_events
                where delivery_synced = false
                  and normalized_status in ('FAILED', 'DELIVERED', 'RETRY_SCHEDULED')
                  and duplicate_event = false
                  and received_at >= current_timestamp - interval '1 hour'
                """, Long.class);
        if (unsyncedFailures != null && unsyncedFailures >= 3) {
            upsertAlert(
                    activeKeys,
                    "webhook-sync-failure",
                    "WARNING",
                    "Webhook delivery sync needs review",
                    "%d provider webhook(s) could not be matched to delivery records.".formatted(unsyncedFailures),
                    "provider_webhook",
                    null,
                    "webhook-failures",
                    Map.of("unsyncedFailures", unsyncedFailures)
            );
        }
    }

    private void evaluateReportRules(List<String> activeKeys) {
        Long failedReports = jdbcTemplate.queryForObject("""
                select count(*) from worker_job_logs
                where status <> 'SUCCESS'
                  and (job_type ilike '%REPORT%' or job_name ilike '%REPORT%')
                  and started_at >= current_timestamp - interval '2 hours'
                """, Long.class);
        if (failedReports != null && failedReports > 0) {
            upsertAlert(
                    activeKeys,
                    "report-generation-failed",
                    "WARNING",
                    "Report generation failures detected",
                    "%d recent report worker run(s) failed.".formatted(failedReports),
                    "worker_job",
                    null,
                    "failed-report-generation",
                    Map.of("failedReports", failedReports)
            );
        }
    }

    private void upsertAlert(
            List<String> activeKeys,
            String alertKey,
            String severity,
            String title,
            String summary,
            String sourceType,
            String sourceId,
            String runbookSlug,
            Map<String, ?> metadata
    ) {
        activeKeys.add(alertKey);
        String dedupeHash = sha256(alertKey);
        UUID existing = jdbcTemplate.query("""
                select id from operational_alerts
                where dedupe_hash = ? and status in ('ACTIVE', 'ACKNOWLEDGED')
                order by created_at desc
                limit 1
                """, rs -> rs.next() ? rs.getObject("id", UUID.class) : null, dedupeHash);
        if (existing == null) {
            jdbcTemplate.update("""
                    insert into operational_alerts (
                        id, alert_key, severity, status, title, summary, source_type, source_id, runbook_slug,
                        dedupe_hash, first_seen_at, last_seen_at, metadata_json, created_at, updated_at
                    ) values (?, ?, ?, 'ACTIVE', ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, ?, current_timestamp, current_timestamp)
                    """,
                    UUID.randomUUID(),
                    alertKey,
                    severity,
                    title,
                    summary,
                    sourceType,
                    sourceId,
                    runbookSlug,
                    dedupeHash,
                    writeJson(metadata)
            );
            return;
        }
        jdbcTemplate.update("""
                update operational_alerts
                set severity = ?, title = ?, summary = ?, source_type = ?, source_id = ?, runbook_slug = ?,
                    last_seen_at = current_timestamp, metadata_json = ?, updated_at = current_timestamp
                where id = ?
                """, severity, title, summary, sourceType, sourceId, runbookSlug, writeJson(metadata), existing);
    }

    private void resolveClearedAlerts(List<String> activeKeys) {
        if (activeKeys.isEmpty()) {
            jdbcTemplate.update("""
                    update operational_alerts
                    set status = 'RESOLVED', resolved_at = current_timestamp, updated_at = current_timestamp
                    where status in ('ACTIVE', 'ACKNOWLEDGED')
                    """);
            return;
        }
        LinkedHashSet<String> uniqueKeys = new LinkedHashSet<>(activeKeys);
        String placeholders = String.join(",", uniqueKeys.stream().map(item -> "?").toList());
        List<Object> params = new ArrayList<>(uniqueKeys);
        jdbcTemplate.update("""
                update operational_alerts
                set status = 'RESOLVED', resolved_at = current_timestamp, updated_at = current_timestamp
                where status in ('ACTIVE', 'ACKNOWLEDGED') and alert_key not in (%s)
                """.formatted(placeholders), params.toArray());
    }

    private void aggregateIncidents() {
        List<IncidentCandidate> candidates = jdbcTemplate.query("""
                select coalesce(runbook_slug, source_type) as incident_key,
                       case when count(*) filter (where severity = 'CRITICAL') > 0 then 'CRITICAL' else 'WARNING' end as severity,
                       max(title) as title,
                       count(*) as alert_count,
                       max(last_seen_at) as last_seen_at,
                       min(source_type) as source_type,
                       min(source_id) as source_id
                from operational_alerts
                where status in ('ACTIVE', 'ACKNOWLEDGED') and severity in ('CRITICAL', 'WARNING')
                group by coalesce(runbook_slug, source_type)
                """, (rs, rowNum) -> new IncidentCandidate(
                rs.getString("incident_key"),
                rs.getString("severity"),
                rs.getString("title"),
                rs.getInt("alert_count"),
                instant(rs, "last_seen_at"),
                rs.getString("source_type"),
                rs.getString("source_id")
        ));
        List<String> openKeys = new ArrayList<>();
        for (IncidentCandidate candidate : candidates) {
            openKeys.add(candidate.incidentKey());
            UUID existing = jdbcTemplate.query("""
                    select id from incident_logs where incident_key = ? and status = 'OPEN' limit 1
                    """, rs -> rs.next() ? rs.getObject("id", UUID.class) : null, candidate.incidentKey());
            String summary = "%d active alert(s) are grouped under %s.".formatted(candidate.alertCount(), candidate.incidentKey());
            if (existing == null) {
                jdbcTemplate.update("""
                        insert into incident_logs (
                            id, incident_key, severity, status, title, summary, primary_source_type, primary_source_id,
                            alert_count, opened_at, last_event_at, metadata_json, created_at, updated_at
                        ) values (?, ?, ?, 'OPEN', ?, ?, ?, ?, ?, current_timestamp, ?, ?, current_timestamp, current_timestamp)
                        """,
                        UUID.randomUUID(),
                        candidate.incidentKey(),
                        candidate.severity(),
                        candidate.title(),
                        summary,
                        candidate.sourceType(),
                        candidate.sourceId(),
                        candidate.alertCount(),
                        candidate.lastSeenAt(),
                        writeJson(Map.of("alertCount", candidate.alertCount()))
                );
            } else {
                jdbcTemplate.update("""
                        update incident_logs
                        set severity = ?, title = ?, summary = ?, primary_source_type = ?, primary_source_id = ?,
                            alert_count = ?, last_event_at = ?, metadata_json = ?, updated_at = current_timestamp
                        where id = ?
                        """,
                        candidate.severity(),
                        candidate.title(),
                        summary,
                        candidate.sourceType(),
                        candidate.sourceId(),
                        candidate.alertCount(),
                        candidate.lastSeenAt(),
                        writeJson(Map.of("alertCount", candidate.alertCount())),
                        existing
                );
            }
        }
        resolveClosedIncidents(openKeys);
    }

    private void resolveClosedIncidents(List<String> openKeys) {
        if (openKeys.isEmpty()) {
            jdbcTemplate.update("""
                    update incident_logs
                    set status = 'RESOLVED', resolved_at = current_timestamp, updated_at = current_timestamp
                    where status = 'OPEN'
                    """);
            return;
        }
        String placeholders = String.join(",", openKeys.stream().map(item -> "?").toList());
        jdbcTemplate.update("""
                update incident_logs
                set status = 'RESOLVED', resolved_at = current_timestamp, updated_at = current_timestamp
                where status = 'OPEN' and incident_key not in (%s)
                """.formatted(placeholders), openKeys.toArray());
    }

    private OperationalAlertResponse alertRow(ResultSet rs, int rowNum) throws SQLException {
        return new OperationalAlertResponse(
                rs.getObject("id", UUID.class),
                rs.getString("alert_key"),
                rs.getString("severity"),
                rs.getString("status"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("source_type"),
                rs.getString("source_id"),
                rs.getString("runbook_slug"),
                instant(rs, "first_seen_at"),
                instant(rs, "last_seen_at"),
                instant(rs, "acknowledged_at"),
                rs.getString("acknowledged_by_email"),
                rs.getString("acknowledgment_note"),
                instant(rs, "resolved_at"),
                rs.getString("metadata_json")
        );
    }

    private IncidentLogResponse incidentRow(ResultSet rs, int rowNum) throws SQLException {
        return new IncidentLogResponse(
                rs.getObject("id", UUID.class),
                rs.getString("incident_key"),
                rs.getString("severity"),
                rs.getString("status"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("primary_source_type"),
                rs.getString("primary_source_id"),
                rs.getInt("alert_count"),
                instant(rs, "opened_at"),
                instant(rs, "last_event_at"),
                instant(rs, "acknowledged_at"),
                instant(rs, "resolved_at"),
                rs.getString("metadata_json")
        );
    }

    private RunbookEntryResponse runbookRow(ResultSet rs, int rowNum) throws SQLException {
        return new RunbookEntryResponse(
                rs.getObject("id", UUID.class),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("severity"),
                rs.getString("category"),
                rs.getString("summary"),
                rs.getString("symptoms"),
                rs.getString("diagnosis_steps"),
                rs.getString("mitigation_steps"),
                rs.getString("escalation_notes"),
                rs.getString("related_alert_keys"),
                instant(rs, "updated_at")
        );
    }

    private ProviderWebhookEventResponse webhookRow(ResultSet rs, int rowNum) throws SQLException {
        return new ProviderWebhookEventResponse(
                rs.getObject("id", UUID.class),
                rs.getString("provider"),
                rs.getString("channel"),
                rs.getString("event_type"),
                rs.getString("normalized_status"),
                rs.getString("provider_event_id"),
                rs.getString("provider_message_id"),
                rs.getBoolean("signature_valid"),
                rs.getBoolean("duplicate_event"),
                rs.getObject("replay_of_event_id", UUID.class),
                rs.getBoolean("delivery_synced"),
                rs.getObject("notification_delivery_id", UUID.class),
                rs.getString("failure_reason"),
                instant(rs, "received_at"),
                instant(rs, "processed_at")
        );
    }

    private DeliveryTimelineEventResponse timelineRow(ResultSet rs, int rowNum) throws SQLException {
        return new DeliveryTimelineEventResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("notification_delivery_id", UUID.class),
                rs.getString("source"),
                rs.getString("provider"),
                rs.getString("event_type"),
                rs.getString("status"),
                rs.getString("message"),
                instant(rs, "observed_at")
        );
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write reliability metadata.", exception);
        }
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
            throw new IllegalStateException("Could not hash alert key.", exception);
        }
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private record IncidentCandidate(
            String incidentKey,
            String severity,
            String title,
            int alertCount,
            Instant lastSeenAt,
            String sourceType,
            String sourceId
    ) {
    }
}
