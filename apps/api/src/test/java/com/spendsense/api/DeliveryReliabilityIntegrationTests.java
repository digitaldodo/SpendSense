package com.spendsense.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.spendsense.api.service.delivery.ProviderWebhookService;
import com.spendsense.api.service.delivery.WorkerQueueService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DeliveryReliabilityIntegrationTests {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProviderWebhookService providerWebhookService;

    @Autowired
    private WorkerQueueService workerQueueService;

    @BeforeEach
    void createOperationalTables() {
        jdbcTemplate.execute("""
                create table if not exists provider_webhook_events (
                    id uuid primary key,
                    provider varchar(80) not null,
                    channel varchar(32) not null default 'EMAIL',
                    event_type varchar(96) not null,
                    normalized_status varchar(32) not null,
                    provider_event_id varchar(180),
                    provider_message_id varchar(180),
                    payload_sha256 varchar(64) not null,
                    signature_valid boolean not null default false,
                    duplicate_event boolean not null default false,
                    replay_of_event_id uuid,
                    delivery_synced boolean not null default false,
                    notification_delivery_id uuid,
                    failure_reason varchar(720),
                    source_ip varchar(80),
                    headers_json text,
                    payload_json text not null,
                    received_at timestamp not null default current_timestamp,
                    processed_at timestamp,
                    created_at timestamp not null default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists worker_queues (
                    id uuid primary key,
                    queue_name varchar(80) not null,
                    job_type varchar(80) not null,
                    status varchar(32) not null,
                    priority integer not null default 100,
                    scheduled_for timestamp not null default current_timestamp,
                    locked_by varchar(120),
                    locked_until timestamp,
                    attempt_count integer not null default 0,
                    max_attempts integer not null default 3,
                    payload_json text not null,
                    idempotency_key varchar(180),
                    trace_id varchar(120),
                    last_error_code varchar(80),
                    last_error_message varchar(720),
                    enqueued_at timestamp not null default current_timestamp,
                    started_at timestamp,
                    completed_at timestamp,
                    failed_at timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table if not exists dead_letter_jobs (
                    id uuid primary key,
                    worker_queue_id uuid,
                    queue_name varchar(80) not null,
                    job_type varchar(80) not null,
                    failed_status varchar(32) not null,
                    attempt_count integer not null,
                    payload_json text not null,
                    failure_code varchar(80),
                    failure_message varchar(720),
                    trace_id varchar(120),
                    exhausted_at timestamp not null default current_timestamp,
                    retried_from_dead_letter_at timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp
                )
                """);
        jdbcTemplate.update("delete from provider_webhook_events");
        jdbcTemplate.update("delete from dead_letter_jobs");
        jdbcTemplate.update("delete from worker_queues");
    }

    @Test
    void webhookReplayIsAuditedAndLinkedToOriginalEvent() {
        String payload = "{\"id\":\"evt-phase-13\",\"type\":\"email.delivered\",\"data\":{}}";

        var first = providerWebhookService.ingest("resend", payload, Map.of(), "127.0.0.1");
        var second = providerWebhookService.ingest("resend", payload, Map.of(), "127.0.0.1");

        assertThat(first.duplicateEvent()).isFalse();
        assertThat(second.duplicateEvent()).isTrue();
        UUID replayOf = jdbcTemplate.queryForObject(
                "select replay_of_event_id from provider_webhook_events where id = ?",
                UUID.class,
                second.webhookEventId()
        );
        assertThat(replayOf).isEqualTo(first.webhookEventId());
    }

    @Test
    void workerClaimReleasesExpiredLocksAndAppliesRetryBackoff() {
        UUID staleJobId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into worker_queues (
                    id, queue_name, job_type, status, priority, scheduled_for, locked_by, locked_until,
                    attempt_count, max_attempts, payload_json, trace_id, enqueued_at, created_at, updated_at
                ) values (?, 'delivery', 'SEND_EMAIL', 'RUNNING', 10, current_timestamp, 'stale-worker', ?,
                    1, 3, '{\"deliveryId\":\"00000000-0000-4000-8000-000000000001\"}',
                    'trace-stale', current_timestamp, current_timestamp, current_timestamp)
                """, staleJobId, Instant.now().minusSeconds(300));

        var claimed = workerQueueService.claimDue("delivery", 5, "phase-13-worker");

        assertThat(claimed).hasSize(1);
        assertThat(claimed.getFirst().id()).isEqualTo(staleJobId);
        workerQueueService.retry(claimed.getFirst(), "TRANSIENT", "Provider temporarily unavailable");
        Instant scheduledFor = jdbcTemplate.queryForObject(
                "select scheduled_for from worker_queues where id = ?",
                Instant.class,
                staleJobId
        );
        assertThat(scheduledFor).isAfter(Instant.now());
    }
}
