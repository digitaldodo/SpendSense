package com.spendsense.api.service.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.dto.engagement.SystemStatusResponse;
import com.spendsense.api.dto.engagement.WorkerJobLogResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class WorkerObservabilityService {
    private static final Logger log = LoggerFactory.getLogger(WorkerObservabilityService.class);
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public WorkerObservabilityService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    public UUID startJob(String jobName, String jobType, Map<String, ?> metadata) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into worker_job_logs (
                    id, job_name, job_type, status, started_at, heartbeat_at, metadata_json, created_at, updated_at
                ) values (?, ?, ?, 'RUNNING', current_timestamp, current_timestamp, ?, current_timestamp, current_timestamp)
                """, id, jobName, jobType, writeJson(metadata));
        log.info("worker_job_started jobName={} jobType={} jobId={}", jobName, jobType, id);
        return id;
    }

    public void finishJob(UUID jobId, String status, int scanned, int succeeded, int failed, String errorMessage) {
        jdbcTemplate.update("""
                update worker_job_logs
                set status = ?, finished_at = current_timestamp,
                    duration_ms = cast(extract(epoch from (current_timestamp - started_at)) * 1000 as bigint),
                    records_scanned = ?, records_succeeded = ?, records_failed = ?,
                    heartbeat_at = current_timestamp, error_message = ?, updated_at = current_timestamp
                where id = ?
                """, status, scanned, succeeded, failed, trim(errorMessage, 720), jobId);
        recordMetric("worker.%s.%s".formatted(jobId, status.toLowerCase()), 1, "count", status.equals("SUCCESS") ? "OK" : "DEGRADED", Map.of("jobId", jobId.toString()));
        log.info("worker_job_finished jobId={} status={} scanned={} succeeded={} failed={}", jobId, status, scanned, succeeded, failed);
    }

    public void heartbeat(UUID jobId) {
        jdbcTemplate.update("update worker_job_logs set heartbeat_at = current_timestamp, updated_at = current_timestamp where id = ?", jobId);
    }

    public void recordMetric(String metricName, double value, String unit, String status, Map<String, ?> dimensions) {
        jdbcTemplate.update("""
                insert into system_health_metrics (
                    id, metric_name, metric_value, metric_unit, status, dimensions_json, observed_at, created_at
                ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """, UUID.randomUUID(), metricName, value, unit, status, writeJson(dimensions));
    }

    public SystemStatusResponse systemStatus() {
        Instant cutoff = Instant.now(clock).minus(Duration.ofHours(24));
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from notification_deliveries where created_at >= ?",
                Long.class,
                cutoff
        );
        Long failed = jdbcTemplate.queryForObject("""
                select count(*) from notification_deliveries
                where created_at >= ? and status in ('FAILED', 'RETRY_SCHEDULED')
                """, Long.class, cutoff);
        Long pendingRetries = jdbcTemplate.queryForObject("""
                select count(*) from notification_deliveries
                where status = 'RETRY_SCHEDULED' and next_retry_at <= current_timestamp
                """, Long.class);
        Instant heartbeat = jdbcTemplate.query("""
                select heartbeat_at from worker_job_logs
                order by heartbeat_at desc
                limit 1
                """, rs -> rs.next() ? instant(rs, "heartbeat_at") : null);
        long totalCount = total == null ? 0 : total;
        long failedCount = failed == null ? 0 : failed;
        double successRate = totalCount == 0 ? 1 : (double) (totalCount - failedCount) / totalCount;
        String status = heartbeat == null || heartbeat.isBefore(Instant.now(clock).minus(Duration.ofHours(2)))
                ? "WAITING"
                : failedCount > Math.max(3, totalCount / 2) ? "DEGRADED" : "OK";
        return new SystemStatusResponse(
                status,
                Instant.now(clock),
                heartbeat,
                totalCount,
                failedCount,
                pendingRetries == null ? 0 : pendingRetries,
                Math.round(successRate * 10000.0) / 100.0,
                recentJobs(8)
        );
    }

    public List<WorkerJobLogResponse> recentJobs(int limit) {
        return jdbcTemplate.query("""
                select * from worker_job_logs
                order by started_at desc
                limit ?
                """, this::jobLogRow, limit);
    }

    private WorkerJobLogResponse jobLogRow(ResultSet rs, int rowNum) throws SQLException {
        Long duration = rs.getObject("duration_ms", Long.class);
        return new WorkerJobLogResponse(
                rs.getObject("id", UUID.class),
                rs.getString("job_name"),
                rs.getString("job_type"),
                rs.getString("status"),
                instant(rs, "started_at"),
                instant(rs, "finished_at"),
                duration,
                rs.getInt("records_scanned"),
                rs.getInt("records_succeeded"),
                rs.getInt("records_failed"),
                instant(rs, "heartbeat_at"),
                rs.getString("error_message")
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
            throw new IllegalStateException("Could not write worker metadata.", exception);
        }
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
