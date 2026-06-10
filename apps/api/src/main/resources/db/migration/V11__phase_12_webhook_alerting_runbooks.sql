create table provider_webhook_events (
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
    replay_of_event_id uuid references provider_webhook_events (id) on delete set null,
    delivery_synced boolean not null default false,
    notification_delivery_id uuid references notification_deliveries (id) on delete set null,
    failure_reason varchar(720),
    source_ip varchar(80),
    headers_json text,
    payload_json text not null,
    received_at timestamptz not null default now(),
    processed_at timestamptz,
    created_at timestamptz not null default now()
);

create table operational_alerts (
    id uuid primary key,
    alert_key varchar(220) not null,
    severity varchar(24) not null,
    status varchar(32) not null default 'ACTIVE',
    title varchar(180) not null,
    summary varchar(720) not null,
    source_type varchar(80) not null,
    source_id varchar(180),
    runbook_slug varchar(120),
    dedupe_hash varchar(64) not null,
    first_seen_at timestamptz not null default now(),
    last_seen_at timestamptz not null default now(),
    acknowledged_at timestamptz,
    acknowledged_by uuid,
    acknowledged_by_email varchar(320),
    acknowledgment_note varchar(520),
    resolved_at timestamptz,
    metadata_json text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table incident_logs (
    id uuid primary key,
    incident_key varchar(220) not null,
    severity varchar(24) not null,
    status varchar(32) not null default 'OPEN',
    title varchar(180) not null,
    summary varchar(900) not null,
    primary_source_type varchar(80) not null,
    primary_source_id varchar(180),
    alert_count integer not null default 0,
    opened_at timestamptz not null default now(),
    last_event_at timestamptz not null default now(),
    acknowledged_at timestamptz,
    resolved_at timestamptz,
    metadata_json text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table runbook_entries (
    id uuid primary key,
    slug varchar(120) not null,
    title varchar(180) not null,
    severity varchar(24) not null,
    category varchar(80) not null,
    summary varchar(620) not null,
    symptoms text not null,
    diagnosis_steps text not null,
    mitigation_steps text not null,
    escalation_notes text,
    related_alert_keys text,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_provider_webhook_events_provider_received
    on provider_webhook_events (provider, received_at desc);
create index idx_provider_webhook_events_message
    on provider_webhook_events (provider_message_id, received_at desc);
create index idx_provider_webhook_events_payload_hash
    on provider_webhook_events (provider, payload_sha256, received_at desc);
create index idx_operational_alerts_status_severity
    on operational_alerts (status, severity, last_seen_at desc);
create unique index ux_operational_alerts_active_dedupe
    on operational_alerts (dedupe_hash)
    where status in ('ACTIVE', 'ACKNOWLEDGED');
create index idx_incident_logs_status_severity
    on incident_logs (status, severity, last_event_at desc);
create unique index ux_incident_logs_open_key
    on incident_logs (incident_key)
    where status = 'OPEN';
create unique index ux_runbook_entries_slug
    on runbook_entries (slug);
create index idx_runbook_entries_search
    on runbook_entries (category, severity, active);

insert into runbook_entries (
    id, slug, title, severity, category, summary, symptoms, diagnosis_steps, mitigation_steps, escalation_notes, related_alert_keys
) values
(
    '8b9b2e89-3ac8-4c3e-9f18-40a2aa817101',
    'provider-outage',
    'Provider outage',
    'CRITICAL',
    'delivery',
    'Use when Resend, SMTP fallback, or a future push provider starts failing enough traffic to degrade delivery confidence.',
    'Provider status is DEGRADED or OUTAGE; delivery webhooks show bounced, delayed, or failed events; active alert mentions provider failure rate.',
    'Check provider status cards; inspect recent provider delivery events; compare webhook failures against send-attempt failures; verify fallback provider availability.',
    'Pause non-critical sends if failures are widespread; enable or switch fallback provider; retry only after provider health returns; keep the incident open until success rate normalizes.',
    'Escalate to engineering lead when the primary and fallback providers fail together or customer-facing report delivery is blocked for more than 30 minutes.',
    'provider-outage,provider-degraded,webhook-failure'
),
(
    '8b9b2e89-3ac8-4c3e-9f18-40a2aa817102',
    'queue-backlog',
    'Queue backlog',
    'WARNING',
    'worker',
    'Use when delivery or report queues are lagging and users may see delayed notifications or exports.',
    'Queue lag exceeds threshold; ready jobs accumulate; throughput falls below normal while pending jobs continue to rise.',
    'Open queue health cards; check oldest scheduled job age; review worker job logs and heartbeat freshness; scan recent errors for shared failure codes.',
    'Restart stuck workers; reduce batch size if downstream services are saturated; retry only jobs with transient errors; keep failed payloads available for audit.',
    'Escalate when lag exceeds one hour, dead-letter count rises, or the queue is blocking scheduled reports.',
    'queue-backlog,queue-degraded,stale-worker'
),
(
    '8b9b2e89-3ac8-4c3e-9f18-40a2aa817103',
    'retry-exhaustion',
    'Retry exhaustion',
    'CRITICAL',
    'delivery',
    'Use when deliveries or worker jobs have consumed all retry attempts and need controlled operator action.',
    'Dead-letter jobs are present; delivery records show FAILED after maximum attempts; retry exhaustion alert appears.',
    'Open the exhausted job or delivery; read last error and trace id; compare provider events and webhook timeline; confirm whether the payload is safe to replay.',
    'Fix the upstream cause before retry; acknowledge the alert with the action taken; use admin retry for known transient failures only; leave permanent failures documented.',
    'Escalate if the same job type exhausts retries repeatedly or customer-visible reports cannot be delivered.',
    'retry-exhausted,dead-letter'
),
(
    '8b9b2e89-3ac8-4c3e-9f18-40a2aa817104',
    'webhook-failures',
    'Webhook failures',
    'WARNING',
    'webhook',
    'Use when provider webhook signatures, payloads, or delivery synchronization are failing.',
    'Webhook events show invalid signatures, unsupported payloads, duplicate replays, or unsynced delivery states.',
    'Verify provider secret configuration; compare provider event id and message id; inspect raw payload audit log; confirm the event mapping for the provider.',
    'Rotate or correct webhook secret; replay provider events after verification is fixed; add provider mapping before accepting new event types as terminal.',
    'Escalate if signature failures continue after secret rotation or delivery state is no longer trustworthy.',
    'webhook-failure,webhook-invalid-signature'
),
(
    '8b9b2e89-3ac8-4c3e-9f18-40a2aa817105',
    'failed-report-generation',
    'Failed report generation',
    'WARNING',
    'reporting',
    'Use when scheduled report generation or delivery jobs fail before users receive exports.',
    'Worker logs show report generation failures; delivery records remain pending; generated report links are missing.',
    'Review worker job logs; check queue payload and trace id; verify source financial data availability; inspect report delivery logs.',
    'Retry after correcting data or template failures; mark permanent failures with an operator note; notify support if a promised report is delayed.',
    'Escalate when reports fail for multiple users, monthly scheduled reports are blocked, or generated files are corrupted.',
    'report-generation-failed,queue-degraded'
),
(
    '8b9b2e89-3ac8-4c3e-9f18-40a2aa817106',
    'stuck-workers',
    'Stuck workers',
    'CRITICAL',
    'worker',
    'Use when worker heartbeats stop or running jobs appear locked beyond the expected lease window.',
    'Latest heartbeat is stale; RUNNING queue jobs have expired locks; worker job log remains RUNNING without progress.',
    'Check heartbeat timestamp; inspect locked_by and locked_until values; compare running job count with active worker processes; review recent deployment changes.',
    'Restart unhealthy workers; release expired locks by retrying affected queue jobs; keep payloads intact; monitor lag and dead-letter count after recovery.',
    'Escalate immediately if all delivery workers are stale in production or locks repeatedly expire after restart.',
    'stale-worker,worker-lock-expired,queue-backlog'
);

comment on table provider_webhook_events is 'Append-only provider webhook audit trail with replay detection and normalized delivery synchronization state.';
comment on table operational_alerts is 'Acknowledgable operational alert history generated from reliability rules.';
comment on table incident_logs is 'Aggregated incident timeline derived from active operational alerts.';
comment on table runbook_entries is 'Admin-only operational runbooks for delivery, webhook, queue, and worker incidents.';
