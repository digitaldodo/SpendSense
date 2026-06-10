package com.spendsense.api.service.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spendsense.api.config.SpendSenseProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkerQueueService {
    private static final Logger log = LoggerFactory.getLogger(WorkerQueueService.class);
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SpendSenseProperties properties;
    private final Clock clock;

    public WorkerQueueService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, SpendSenseProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public UUID enqueue(
            String queueName,
            String jobType,
            Map<String, ?> payload,
            String idempotencyKey,
            Instant scheduledFor,
            int priority,
            int maxAttempts,
            String traceId
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into worker_queues (
                    id, queue_name, job_type, status, priority, scheduled_for, attempt_count, max_attempts,
                    payload_json, idempotency_key, trace_id, enqueued_at, created_at, updated_at
                ) values (?, ?, ?, 'PENDING', ?, ?, 0, ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """,
                id,
                queueName,
                jobType,
                priority,
                scheduledFor == null ? Instant.now(clock) : scheduledFor,
                maxAttempts,
                writeJson(payload),
                idempotencyKey,
                traceId
        );
        log.info("worker_queue_enqueued queue={} jobType={} jobId={} traceId={}", queueName, jobType, id, traceId);
        return id;
    }

    @Transactional
    public List<WorkerQueueJob> claimDue(String queueName, int limit, String workerId) {
        releaseExpiredLocks(queueName);
        List<WorkerQueueJob> jobs = jdbcTemplate.query("""
                select * from worker_queues
                where queue_name = ?
                  and status in ('PENDING', 'RETRY_SCHEDULED')
                  and scheduled_for <= current_timestamp
                  and (locked_until is null or locked_until < current_timestamp)
                order by priority asc, scheduled_for asc, enqueued_at asc
                limit ?
                for update skip locked
                """, this::queueRow, queueName, limit);
        Instant lockedUntil = Instant.now(clock).plusSeconds(workerLockTtlSeconds());
        for (WorkerQueueJob job : jobs) {
            jdbcTemplate.update("""
                    update worker_queues
                    set status = 'RUNNING', locked_by = ?, locked_until = ?, started_at = coalesce(started_at, current_timestamp),
                        attempt_count = attempt_count + 1, updated_at = current_timestamp
                    where id = ?
                    """, workerId, lockedUntil, job.id());
        }
        if (!jobs.isEmpty()) {
            log.info("worker_queue_claimed queue={} workerId={} count={}", queueName, workerId, jobs.size());
        }
        return jobs.stream().map(job -> new WorkerQueueJob(
                job.id(),
                job.queueName(),
                job.jobType(),
                "RUNNING",
                job.priority(),
                job.scheduledFor(),
                job.attemptCount() + 1,
                job.maxAttempts(),
                job.payloadJson(),
                job.traceId()
        )).toList();
    }

    @Transactional
    public void complete(UUID queueJobId) {
        jdbcTemplate.update("""
                update worker_queues
                set status = 'COMPLETED', locked_by = null, locked_until = null, completed_at = current_timestamp,
                    updated_at = current_timestamp
                where id = ?
                """, queueJobId);
    }

    @Transactional
    public void retry(WorkerQueueJob job, String errorCode, String errorMessage) {
        Instant scheduledFor = Instant.now(clock).plusSeconds(retryBackoffSeconds(job));
        jdbcTemplate.update("""
                update worker_queues
                set status = 'RETRY_SCHEDULED', locked_by = null, locked_until = null, scheduled_for = ?,
                    last_error_code = ?, last_error_message = ?, updated_at = current_timestamp
                where id = ?
                """, scheduledFor, trim(errorCode, 80), trim(errorMessage, 720), job.id());
    }

    @Transactional
    public void deadLetter(WorkerQueueJob job, String errorCode, String errorMessage) {
        jdbcTemplate.update("""
                update worker_queues
                set status = 'DEAD_LETTER', locked_by = null, locked_until = null, failed_at = current_timestamp,
                    last_error_code = ?, last_error_message = ?, updated_at = current_timestamp
                where id = ?
                """, trim(errorCode, 80), trim(errorMessage, 720), job.id());
        jdbcTemplate.update("""
                insert into dead_letter_jobs (
                    id, worker_queue_id, queue_name, job_type, failed_status, attempt_count, payload_json,
                    failure_code, failure_message, trace_id, exhausted_at, created_at, updated_at
                ) values (?, ?, ?, ?, 'DEAD_LETTER', ?, ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """,
                UUID.randomUUID(),
                job.id(),
                job.queueName(),
                job.jobType(),
                job.attemptCount(),
                job.payloadJson(),
                trim(errorCode, 80),
                trim(errorMessage, 720),
                job.traceId()
        );
        log.warn("worker_queue_dead_lettered queue={} jobType={} jobId={} errorCode={}", job.queueName(), job.jobType(), job.id(), errorCode);
    }

    @Transactional
    public void retryQueueJob(UUID queueJobId) {
        jdbcTemplate.update("""
                update worker_queues
                set status = 'PENDING', scheduled_for = current_timestamp, locked_by = null, locked_until = null,
                    failed_at = null, last_error_code = null, last_error_message = null, updated_at = current_timestamp
                where id = ? and status in ('FAILED', 'DEAD_LETTER', 'RETRY_SCHEDULED')
                """, queueJobId);
    }

    @Transactional
    public void retryDeadLetter(UUID deadLetterId) {
        UUID queueJobId = jdbcTemplate.query("""
                select worker_queue_id from dead_letter_jobs where id = ?
                """, rs -> rs.next() ? rs.getObject("worker_queue_id", UUID.class) : null, deadLetterId);
        if (queueJobId == null) {
            throw new IllegalArgumentException("Dead-letter job is no longer linked to a queue record.");
        }
        retryQueueJob(queueJobId);
        jdbcTemplate.update("""
                update dead_letter_jobs
                set retried_from_dead_letter_at = current_timestamp, updated_at = current_timestamp
                where id = ?
                """, deadLetterId);
    }

    @Transactional
    public int cleanupCompletedJobs() {
        Integer retentionDays = properties.delivery().worker().cleanupRetentionDays();
        int days = retentionDays == null ? 30 : Math.max(1, retentionDays);
        return jdbcTemplate.update("""
                delete from worker_queues
                where status = 'COMPLETED'
                  and completed_at < current_timestamp - (? * interval '1 day')
                """, days);
    }

    public Map<String, Object> readPayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, Map.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Queue payload is not valid JSON.", exception);
        }
    }

    private WorkerQueueJob queueRow(ResultSet rs, int rowNum) throws SQLException {
        return new WorkerQueueJob(
                rs.getObject("id", UUID.class),
                rs.getString("queue_name"),
                rs.getString("job_type"),
                rs.getString("status"),
                rs.getInt("priority"),
                instant(rs, "scheduled_for"),
                rs.getInt("attempt_count"),
                rs.getInt("max_attempts"),
                rs.getString("payload_json"),
                rs.getString("trace_id")
        );
    }

    private long retryDelaySeconds() {
        Integer value = properties.delivery().worker().retryDelaySeconds();
        return value == null ? 300 : Math.max(30, value);
    }

    private long retryBackoffSeconds(WorkerQueueJob job) {
        long baseDelay = retryDelaySeconds();
        int exponent = Math.min(Math.max(0, job.attemptCount() - 1), 5);
        long jitter = Math.abs(job.id().getLeastSignificantBits() % Math.max(1, baseDelay / 3));
        return Math.min(86_400, (baseDelay * (1L << exponent)) + jitter);
    }

    private void releaseExpiredLocks(String queueName) {
        int released = jdbcTemplate.update("""
                update worker_queues
                set status = 'RETRY_SCHEDULED', locked_by = null, locked_until = null,
                    scheduled_for = current_timestamp, last_error_code = 'WORKER_TIMEOUT',
                    last_error_message = 'Worker lock expired before the job completed.', updated_at = current_timestamp
                where queue_name = ?
                  and status = 'RUNNING'
                  and locked_until is not null
                  and locked_until < current_timestamp
                """, queueName);
        if (released > 0) {
            log.warn("worker_queue_expired_locks_released queue={} count={}", queueName, released);
        }
    }

    private long workerLockTtlSeconds() {
        Integer value = properties.delivery().worker().lockTtlSeconds();
        return value == null ? 120 : Math.max(30, value);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not write queue payload.", exception);
        }
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
