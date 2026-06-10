create index idx_transactions_dashboard_range
    on transactions (user_profile_id, status, occurred_at desc)
    include (amount, direction, account_id, category_id);

create index idx_transactions_reporting_monthly
    on transactions (user_profile_id, direction, status, occurred_at)
    include (amount, category_id);

create index idx_transactions_csv_dedupe_lookup
    on transactions (user_profile_id, source, dedupe_fingerprint);

create index idx_import_jobs_user_completed
    on import_jobs (user_profile_id, completed_at desc)
    where completed_at is not null;

create index idx_import_failures_job_severity_row
    on import_failures (import_job_id, severity, row_number);

create index idx_reconciliation_logs_job_created
    on reconciliation_logs (import_job_id, created_at desc);

create index idx_notifications_active_priority_created
    on notifications (user_profile_id, lifecycle_status, priority, created_at desc)
    where lifecycle_status = 'ACTIVE';

create index idx_notifications_actionable_lookup
    on notifications (user_profile_id, source_type, source_id, created_at desc)
    where dismissed_at is null;

create index idx_notification_deliveries_retry_lookup
    on notification_deliveries (status, next_retry_at, created_at)
    where status in ('PENDING', 'RETRY_SCHEDULED');

create index idx_worker_queues_claim_due
    on worker_queues (queue_name, status, scheduled_for, locked_until, priority, enqueued_at);

create index idx_worker_queues_running_timeout
    on worker_queues (queue_name, status, locked_until)
    where status = 'RUNNING';

create index idx_worker_queues_trace
    on worker_queues (trace_id, created_at desc)
    where trace_id is not null;

create index idx_provider_webhook_events_replay_event
    on provider_webhook_events (provider, provider_event_id, received_at asc)
    where provider_event_id is not null;

create index idx_provider_webhook_events_duplicate_audit
    on provider_webhook_events (duplicate_event, received_at desc)
    where duplicate_event = true;

create index idx_operational_alerts_active_seen
    on operational_alerts (status, last_seen_at desc, severity)
    where status in ('ACTIVE', 'ACKNOWLEDGED');

comment on index idx_transactions_dashboard_range is 'Covering index for dashboard transaction counts, recent rows, range totals, and joins.';
comment on index idx_transactions_reporting_monthly is 'Optimizes monthly reporting and category aggregation scans by user, direction, status, and date.';
comment on index idx_worker_queues_claim_due is 'Optimizes queue worker due-job claims under high pending volume.';
