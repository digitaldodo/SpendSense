create table worker_queues (
    id uuid primary key,
    queue_name varchar(80) not null,
    job_type varchar(80) not null,
    status varchar(32) not null,
    priority integer not null default 100,
    scheduled_for timestamptz not null default now(),
    locked_by varchar(120),
    locked_until timestamptz,
    attempt_count integer not null default 0,
    max_attempts integer not null default 3,
    payload_json text not null,
    idempotency_key varchar(180),
    trace_id varchar(120),
    last_error_code varchar(80),
    last_error_message varchar(720),
    enqueued_at timestamptz not null default now(),
    started_at timestamptz,
    completed_at timestamptz,
    failed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_worker_queues_attempts check (attempt_count >= 0 and max_attempts > 0)
);

create table dead_letter_jobs (
    id uuid primary key,
    worker_queue_id uuid references worker_queues (id) on delete set null,
    queue_name varchar(80) not null,
    job_type varchar(80) not null,
    failed_status varchar(32) not null,
    attempt_count integer not null,
    payload_json text not null,
    failure_code varchar(80),
    failure_message varchar(720),
    trace_id varchar(120),
    exhausted_at timestamptz not null default now(),
    retried_from_dead_letter_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table provider_delivery_events (
    id uuid primary key,
    notification_delivery_id uuid references notification_deliveries (id) on delete set null,
    provider varchar(80) not null,
    channel varchar(32) not null,
    event_type varchar(64) not null,
    status varchar(32) not null,
    provider_message_id varchar(160),
    latency_ms bigint,
    error_code varchar(80),
    error_message varchar(720),
    metadata_json text,
    observed_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

create table admin_audit_logs (
    id uuid primary key,
    actor_user_id uuid,
    actor_email varchar(320),
    action varchar(96) not null,
    target_type varchar(80) not null,
    target_id uuid,
    reason varchar(520),
    metadata_json text,
    trace_id varchar(120),
    created_at timestamptz not null default now()
);

create index idx_worker_queues_ready
    on worker_queues (queue_name, status, scheduled_for, priority);
create index idx_worker_queues_locked
    on worker_queues (status, locked_until);
create index idx_worker_queues_type_created
    on worker_queues (job_type, created_at desc);
create unique index idx_worker_queues_idempotency
    on worker_queues (idempotency_key)
    where idempotency_key is not null;
create index idx_dead_letter_jobs_queue_created
    on dead_letter_jobs (queue_name, created_at desc);
create index idx_provider_delivery_events_delivery
    on provider_delivery_events (notification_delivery_id, observed_at desc);
create index idx_provider_delivery_events_provider_status
    on provider_delivery_events (provider, status, observed_at desc);
create index idx_admin_audit_logs_actor_created
    on admin_audit_logs (actor_user_id, created_at desc);
create index idx_admin_audit_logs_target
    on admin_audit_logs (target_type, target_id);

comment on table worker_queues is 'Database-backed queue for horizontally scalable worker leasing, retry, and latency tracking.';
comment on table dead_letter_jobs is 'Terminal worker failures retained for audit-safe inspection and controlled retry.';
comment on table provider_delivery_events is 'Provider-level delivery telemetry for failover, latency, outage, and error aggregation.';
comment on table admin_audit_logs is 'Immutable audit trail for privileged operational actions.';
