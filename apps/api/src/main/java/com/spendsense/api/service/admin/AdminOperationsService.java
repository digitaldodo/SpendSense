package com.spendsense.api.service.admin;

import com.spendsense.api.dto.admin.AdminAuditLogResponse;
import com.spendsense.api.dto.admin.AdminNotificationResponse;
import com.spendsense.api.dto.admin.AdminOperationsOverviewResponse;
import com.spendsense.api.dto.admin.DeadLetterJobResponse;
import com.spendsense.api.dto.admin.DeliveryAnalyticsResponse;
import com.spendsense.api.dto.admin.OperationalTraceEventResponse;
import com.spendsense.api.dto.admin.ProviderDeliveryEventResponse;
import com.spendsense.api.dto.admin.ProviderStatusResponse;
import com.spendsense.api.dto.admin.QueueHealthResponse;
import com.spendsense.api.dto.admin.WorkerQueueResponse;
import com.spendsense.api.security.SupabasePrincipal;
import com.spendsense.api.service.delivery.WorkerObservabilityService;
import com.spendsense.api.service.delivery.WorkerQueueService;
import com.spendsense.api.service.ops.OperationalTraceService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminOperationsService {
    private final JdbcTemplate jdbcTemplate;
    private final WorkerObservabilityService workerObservabilityService;
    private final WorkerQueueService workerQueueService;
    private final AdminAuditService adminAuditService;
    private final OperationalTraceService operationalTraceService;

    public AdminOperationsService(
            JdbcTemplate jdbcTemplate,
            WorkerObservabilityService workerObservabilityService,
            WorkerQueueService workerQueueService,
            AdminAuditService adminAuditService,
            OperationalTraceService operationalTraceService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.workerObservabilityService = workerObservabilityService;
        this.workerQueueService = workerQueueService;
        this.adminAuditService = adminAuditService;
        this.operationalTraceService = operationalTraceService;
    }

    public AdminOperationsOverviewResponse overview() {
        DeliveryAnalyticsResponse analytics = deliveryAnalytics();
        List<QueueHealthResponse> queues = queueHealth();
        List<ProviderStatusResponse> providers = providerStatuses();
        List<AdminNotificationResponse> notifications = adminNotifications(queues, providers, analytics);
        String status = notifications.stream().anyMatch(item -> "ACTION".equals(item.severity())) ? "DEGRADED" : "OK";
        if (queues.isEmpty() && analytics.deliveriesLast24h() == 0) {
            status = "WAITING";
        }
        return new AdminOperationsOverviewResponse(
                status,
                Instant.now(),
                analytics,
                queues,
                providers,
                notifications,
                workerObservabilityService.recentJobs(8)
        );
    }

    public List<WorkerQueueResponse> queueJobs(String status, String queueName, String search, int limit) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select * from worker_queues where 1 = 1");
        if (StringUtils.hasText(status)) {
            sql.append(" and status = ?");
            params.add(status);
        }
        if (StringUtils.hasText(queueName)) {
            sql.append(" and queue_name = ?");
            params.add(queueName);
        }
        if (StringUtils.hasText(search)) {
            sql.append(" and (job_type ilike ? or payload_json ilike ? or coalesce(last_error_message, '') ilike ?)");
            String like = "%" + search + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" order by case status when 'DEAD_LETTER' then 0 when 'RETRY_SCHEDULED' then 1 when 'RUNNING' then 2 else 3 end, updated_at desc limit ?");
        params.add(Math.min(Math.max(limit, 1), 200));
        return jdbcTemplate.query(sql.toString(), this::queueRow, params.toArray());
    }

    public WorkerQueueResponse queueJob(UUID jobId) {
        return jdbcTemplate.queryForObject("select * from worker_queues where id = ?", this::queueRow, jobId);
    }

    public List<DeadLetterJobResponse> deadLetters(String search, int limit) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select * from dead_letter_jobs where 1 = 1");
        if (StringUtils.hasText(search)) {
            sql.append(" and (job_type ilike ? or payload_json ilike ? or coalesce(failure_message, '') ilike ?)");
            String like = "%" + search + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" order by exhausted_at desc limit ?");
        params.add(Math.min(Math.max(limit, 1), 200));
        return jdbcTemplate.query(sql.toString(), this::deadLetterRow, params.toArray());
    }

    public List<ProviderDeliveryEventResponse> providerEvents(String provider, String status, int limit) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select * from provider_delivery_events where 1 = 1");
        if (StringUtils.hasText(provider)) {
            sql.append(" and provider = ?");
            params.add(provider);
        }
        if (StringUtils.hasText(status)) {
            sql.append(" and status = ?");
            params.add(status);
        }
        sql.append(" order by observed_at desc limit ?");
        params.add(Math.min(Math.max(limit, 1), 200));
        return jdbcTemplate.query(sql.toString(), this::providerEventRow, params.toArray());
    }

    public List<AdminAuditLogResponse> auditLogs(int limit) {
        return jdbcTemplate.query("""
                select * from admin_audit_logs
                order by created_at desc
                limit ?
                """, this::auditRow, Math.min(Math.max(limit, 1), 100));
    }

    public List<OperationalTraceEventResponse> traceEvents(String eventType, String severity, String source, int limit) {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("select * from operational_trace_events where 1 = 1");
        if (StringUtils.hasText(eventType)) {
            sql.append(" and event_type = ?");
            params.add(eventType);
        }
        if (StringUtils.hasText(severity)) {
            sql.append(" and severity = ?");
            params.add(severity);
        }
        if (StringUtils.hasText(source)) {
            sql.append(" and source = ?");
            params.add(source);
        }
        sql.append(" order by observed_at desc limit ?");
        params.add(Math.min(Math.max(limit, 1), 200));
        return jdbcTemplate.query(sql.toString(), this::traceEventRow, params.toArray());
    }

    @Transactional
    public WorkerQueueResponse retryQueueJob(SupabasePrincipal principal, UUID jobId, String reason, String traceId) {
        workerQueueService.retryQueueJob(jobId);
        adminAuditService.record(principal, "QUEUE_JOB_RETRY", "worker_queue", jobId, reason, Map.of(), traceId);
        operationalTraceService.record(
                "operator_queue_retry",
                "WARNING",
                "worker_queue",
                jobId.toString(),
                traceId,
                "Operator requested worker queue retry.",
                Map.of("actor", principal.email() == null ? "" : principal.email(), "reason", reason == null ? "" : reason)
        );
        return queueJob(jobId);
    }

    @Transactional
    public DeadLetterJobResponse retryDeadLetter(SupabasePrincipal principal, UUID deadLetterId, String reason, String traceId) {
        workerQueueService.retryDeadLetter(deadLetterId);
        adminAuditService.record(principal, "DEAD_LETTER_RETRY", "dead_letter_job", deadLetterId, reason, Map.of(), traceId);
        operationalTraceService.record(
                "operator_dead_letter_retry",
                "WARNING",
                "dead_letter_job",
                deadLetterId.toString(),
                traceId,
                "Operator requested dead-letter retry.",
                Map.of("actor", principal.email() == null ? "" : principal.email(), "reason", reason == null ? "" : reason)
        );
        return jdbcTemplate.queryForObject("select * from dead_letter_jobs where id = ?", this::deadLetterRow, deadLetterId);
    }

    private DeliveryAnalyticsResponse deliveryAnalytics() {
        Long total = count("""
                select count(*) from notification_deliveries where created_at >= current_timestamp - interval '24 hours'
                """);
        Long delivered = count("""
                select count(*) from notification_deliveries
                where delivered_at >= current_timestamp - interval '24 hours'
                """);
        Long failed = count("""
                select count(*) from notification_deliveries
                where failed_at >= current_timestamp - interval '24 hours'
                """);
        Long retryScheduled = count("select count(*) from notification_deliveries where status = 'RETRY_SCHEDULED'");
        Long exhausted = count("""
                select count(*) from dead_letter_jobs where exhausted_at >= current_timestamp - interval '24 hours'
                """);
        Long averageLatency = jdbcTemplate.queryForObject("""
                select cast(avg(latency_ms) as bigint) from provider_delivery_events
                where observed_at >= current_timestamp - interval '24 hours' and latency_ms is not null
                """, Long.class);
        long totalCount = total == null ? 0 : total;
        long deliveredCount = delivered == null ? 0 : delivered;
        double successRate = totalCount == 0 ? 100 : Math.round(((double) deliveredCount / totalCount) * 10000.0) / 100.0;
        return new DeliveryAnalyticsResponse(
                totalCount,
                deliveredCount,
                failed == null ? 0 : failed,
                retryScheduled == null ? 0 : retryScheduled,
                successRate,
                exhausted == null ? 0 : exhausted,
                averageLatency
        );
    }

    private List<QueueHealthResponse> queueHealth() {
        return jdbcTemplate.query("""
                select
                  queue_name,
                  count(*) filter (where status = 'PENDING' and scheduled_for <= current_timestamp) as ready,
                  count(*) filter (where status = 'RUNNING') as running,
                  count(*) filter (where status = 'RETRY_SCHEDULED') as retry_scheduled,
                  count(*) filter (where status = 'DEAD_LETTER') as dead_letter,
                  coalesce(cast(extract(epoch from (current_timestamp - min(scheduled_for) filter (
                    where status in ('PENDING', 'RETRY_SCHEDULED') and scheduled_for <= current_timestamp
                  ))) as bigint), 0) as lag_seconds,
                  count(*) filter (where status = 'COMPLETED' and completed_at >= current_timestamp - interval '1 hour') as throughput_last_hour,
                  count(*) filter (where status in ('DEAD_LETTER', 'FAILED') and failed_at >= current_timestamp - interval '1 hour') as failures_last_hour
                from worker_queues
                group by queue_name
                order by queue_name
                """, (rs, rowNum) -> new QueueHealthResponse(
                rs.getString("queue_name"),
                rs.getLong("ready"),
                rs.getLong("running"),
                rs.getLong("retry_scheduled"),
                rs.getLong("dead_letter"),
                rs.getLong("lag_seconds"),
                rs.getLong("throughput_last_hour"),
                rs.getLong("failures_last_hour")
        ));
    }

    private List<ProviderStatusResponse> providerStatuses() {
        return jdbcTemplate.query("""
                select
                  provider,
                  channel,
                  count(*) as attempts,
                  count(*) filter (where status <> 'DELIVERED') as failures,
                  cast(avg(latency_ms) as bigint) as average_latency_ms,
                  max(observed_at) as last_event_at,
                  (array_remove(array_agg(error_code order by observed_at desc), null))[1] as last_error_code
                from provider_delivery_events
                where observed_at >= current_timestamp - interval '24 hours'
                group by provider, channel
                order by provider
                """, (rs, rowNum) -> {
            long attempts = rs.getLong("attempts");
            long failures = rs.getLong("failures");
            double successRate = attempts == 0 ? 100 : Math.round(((double) (attempts - failures) / attempts) * 10000.0) / 100.0;
            return new ProviderStatusResponse(
                    rs.getString("provider"),
                    rs.getString("channel"),
                    failures > Math.max(3, attempts / 2) ? "DEGRADED" : "OK",
                    attempts,
                    failures,
                    successRate,
                    rs.getObject("average_latency_ms", Long.class),
                    instant(rs, "last_event_at"),
                    rs.getString("last_error_code")
            );
        });
    }

    private List<AdminNotificationResponse> adminNotifications(
            List<QueueHealthResponse> queues,
            List<ProviderStatusResponse> providers,
            DeliveryAnalyticsResponse analytics
    ) {
        List<AdminNotificationResponse> notifications = new ArrayList<>();
        for (QueueHealthResponse queue : queues) {
            if (queue.deadLetter() > 0) {
                notifications.add(new AdminNotificationResponse(
                        UUID.randomUUID(),
                        "ACTION",
                        "Dead-letter jobs need review",
                        "%s has %d exhausted job(s).".formatted(queue.queueName(), queue.deadLetter()),
                        "worker_queue",
                        null,
                        Instant.now()
                ));
            } else if (queue.lagSeconds() > 900) {
                notifications.add(new AdminNotificationResponse(
                        UUID.randomUUID(),
                        "CAUTION",
                        "Queue lag is elevated",
                        "%s is behind by %d seconds.".formatted(queue.queueName(), queue.lagSeconds()),
                        "worker_queue",
                        null,
                        Instant.now()
                ));
            }
        }
        for (ProviderStatusResponse provider : providers) {
            if ("DEGRADED".equals(provider.status())) {
                notifications.add(new AdminNotificationResponse(
                        UUID.randomUUID(),
                        "ACTION",
                        "Provider failure rate is high",
                        "%s failed %d time(s) in the last 24 hours.".formatted(provider.provider(), provider.failuresLast24h()),
                        "provider",
                        null,
                        Instant.now()
                ));
            }
        }
        if (analytics.retryScheduled() > 0) {
            notifications.add(new AdminNotificationResponse(
                    UUID.randomUUID(),
                    "INFO",
                    "Retries are scheduled",
                    "%d delivery attempt(s) are waiting for retry.".formatted(analytics.retryScheduled()),
                    "notification_delivery",
                    null,
                    Instant.now()
            ));
        }
        return notifications.stream().limit(8).toList();
    }

    private WorkerQueueResponse queueRow(ResultSet rs, int rowNum) throws SQLException {
        Instant enqueuedAt = instant(rs, "enqueued_at");
        Instant completedAt = instant(rs, "completed_at");
        Instant failedAt = instant(rs, "failed_at");
        Instant terminalAt = completedAt == null ? failedAt : completedAt;
        long latencyMs = enqueuedAt == null || terminalAt == null ? 0 : Math.max(0, terminalAt.toEpochMilli() - enqueuedAt.toEpochMilli());
        return new WorkerQueueResponse(
                rs.getObject("id", UUID.class),
                rs.getString("queue_name"),
                rs.getString("job_type"),
                rs.getString("status"),
                rs.getInt("priority"),
                instant(rs, "scheduled_for"),
                rs.getString("locked_by"),
                instant(rs, "locked_until"),
                rs.getInt("attempt_count"),
                rs.getInt("max_attempts"),
                rs.getString("payload_json"),
                rs.getString("trace_id"),
                rs.getString("last_error_code"),
                rs.getString("last_error_message"),
                enqueuedAt,
                instant(rs, "started_at"),
                completedAt,
                failedAt,
                latencyMs,
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private DeadLetterJobResponse deadLetterRow(ResultSet rs, int rowNum) throws SQLException {
        return new DeadLetterJobResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("worker_queue_id", UUID.class),
                rs.getString("queue_name"),
                rs.getString("job_type"),
                rs.getString("failed_status"),
                rs.getInt("attempt_count"),
                rs.getString("payload_json"),
                rs.getString("failure_code"),
                rs.getString("failure_message"),
                rs.getString("trace_id"),
                instant(rs, "exhausted_at"),
                instant(rs, "retried_from_dead_letter_at"),
                instant(rs, "created_at")
        );
    }

    private ProviderDeliveryEventResponse providerEventRow(ResultSet rs, int rowNum) throws SQLException {
        return new ProviderDeliveryEventResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("notification_delivery_id", UUID.class),
                rs.getString("provider"),
                rs.getString("channel"),
                rs.getString("event_type"),
                rs.getString("status"),
                rs.getString("provider_message_id"),
                rs.getObject("latency_ms", Long.class),
                rs.getString("error_code"),
                rs.getString("error_message"),
                instant(rs, "observed_at")
        );
    }

    private AdminAuditLogResponse auditRow(ResultSet rs, int rowNum) throws SQLException {
        return new AdminAuditLogResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("actor_user_id", UUID.class),
                rs.getString("actor_email"),
                rs.getString("action"),
                rs.getString("target_type"),
                rs.getObject("target_id", UUID.class),
                rs.getString("reason"),
                rs.getString("trace_id"),
                instant(rs, "created_at")
        );
    }

    private OperationalTraceEventResponse traceEventRow(ResultSet rs, int rowNum) throws SQLException {
        return new OperationalTraceEventResponse(
                rs.getObject("id", UUID.class),
                rs.getString("event_type"),
                rs.getString("severity"),
                rs.getString("environment"),
                rs.getString("release_version"),
                rs.getString("release_commit"),
                rs.getString("source"),
                rs.getString("source_id"),
                rs.getString("trace_id"),
                rs.getString("message"),
                rs.getString("metadata_json"),
                instant(rs, "observed_at")
        );
    }

    private Long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
