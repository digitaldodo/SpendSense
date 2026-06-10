alter table notification_preferences
    add column email_enabled boolean not null default false,
    add column email_address varchar(320),
    add column digest_frequency varchar(24) not null default 'OFF',
    add column budget_alert_email_enabled boolean not null default false,
    add column recurring_reminder_email_enabled boolean not null default false,
    add column report_email_enabled boolean not null default false,
    add column delivery_failure_alerts_enabled boolean not null default true;

create table notification_deliveries (
    id uuid primary key,
    notification_id uuid references notifications (id) on delete set null,
    scheduled_report_id uuid references scheduled_reports (id) on delete set null,
    generated_report_id uuid references generated_reports (id) on delete set null,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    delivery_kind varchar(64) not null,
    channel varchar(32) not null,
    provider varchar(64) not null,
    recipient varchar(320),
    subject varchar(240),
    status varchar(32) not null,
    attempt_count integer not null default 0,
    next_retry_at timestamptz,
    last_attempt_at timestamptz,
    delivered_at timestamptz,
    failed_at timestamptz,
    provider_message_id varchar(160),
    error_code varchar(80),
    error_message varchar(520),
    trace_id varchar(120),
    payload_json text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_notification_deliveries_attempts check (attempt_count >= 0)
);

create table delivery_retries (
    id uuid primary key,
    notification_delivery_id uuid not null references notification_deliveries (id) on delete cascade,
    attempt_number integer not null,
    scheduled_for timestamptz not null,
    attempted_at timestamptz,
    status varchar(32) not null,
    error_code varchar(80),
    error_message varchar(520),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_delivery_retries_attempt_number check (attempt_number > 0)
);

create table worker_job_logs (
    id uuid primary key,
    job_name varchar(96) not null,
    job_type varchar(64) not null,
    status varchar(32) not null,
    started_at timestamptz not null,
    finished_at timestamptz,
    duration_ms bigint,
    records_scanned integer not null default 0,
    records_succeeded integer not null default 0,
    records_failed integer not null default 0,
    heartbeat_at timestamptz not null,
    error_message varchar(720),
    metadata_json text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table system_health_metrics (
    id uuid primary key,
    metric_name varchar(96) not null,
    metric_value numeric(18,4) not null,
    metric_unit varchar(32) not null default 'count',
    status varchar(32) not null default 'OK',
    dimensions_json text,
    observed_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

create index idx_notification_deliveries_user_created
    on notification_deliveries (user_profile_id, created_at desc);
create index idx_notification_deliveries_status_retry
    on notification_deliveries (status, next_retry_at);
create index idx_notification_deliveries_report
    on notification_deliveries (scheduled_report_id, generated_report_id);
create index idx_delivery_retries_delivery_attempt
    on delivery_retries (notification_delivery_id, attempt_number);
create index idx_worker_job_logs_name_started
    on worker_job_logs (job_name, started_at desc);
create index idx_worker_job_logs_status_started
    on worker_job_logs (status, started_at desc);
create index idx_system_health_metrics_name_observed
    on system_health_metrics (metric_name, observed_at desc);

comment on table notification_deliveries is 'Traceable delivery attempts across email, in-app, and future push channels.';
comment on table delivery_retries is 'Retry schedule and outcome history for failed delivery attempts.';
comment on table worker_job_logs is 'Structured execution log and heartbeat trail for scheduled background workers.';
comment on table system_health_metrics is 'Operational metric snapshots for delivery, ingestion, and worker health monitoring.';
