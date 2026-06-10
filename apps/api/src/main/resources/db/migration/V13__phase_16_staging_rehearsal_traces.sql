create table operational_trace_events (
    id uuid primary key,
    event_type varchar(96) not null,
    severity varchar(24) not null,
    environment varchar(40) not null,
    release_version varchar(120),
    release_commit varchar(120),
    source varchar(120) not null,
    source_id varchar(180),
    trace_id varchar(120),
    message varchar(720) not null,
    metadata_json text,
    observed_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

create index idx_operational_trace_events_observed
    on operational_trace_events (observed_at desc, severity, event_type);

create index idx_operational_trace_events_source
    on operational_trace_events (source, source_id, observed_at desc)
    where source_id is not null;

insert into runbook_entries (
    id, slug, title, severity, category, summary, symptoms, diagnosis_steps, mitigation_steps, escalation_notes, related_alert_keys
) values
(
    '8b9b2e89-3ac8-4c3e-9f18-40a2aa817107',
    'deployment-rehearsal',
    'Deployment rehearsal failure',
    'WARNING',
    'deployment',
    'Use when staging smoke checks, rollback validation, release metadata, or dependency verification does not match the intended release.',
    'Readiness is not UP; release commit is missing; frontend and backend environments disagree; smoke tests fail; maintenance or rollback checks do not settle.',
    'Check /api/v1/health/deployment; compare web release metadata with backend version; review operational trace events; confirm staging secrets and domain targets are isolated.',
    'Stop rollout, keep maintenance mode enabled for unsafe writes, roll back to the previous known-good artifact, then rerun smoke tests before restoring traffic.',
    'Escalate if rollback health does not become ready within the expected provider window or if production credentials appear in staging.',
    'deployment-health,deployment-validation,rollback-failed,maintenance-mode'
);

comment on table operational_trace_events is 'Append-only deployment, startup, worker, rollback, and incident-drill trace events for operational rehearsal evidence.';
