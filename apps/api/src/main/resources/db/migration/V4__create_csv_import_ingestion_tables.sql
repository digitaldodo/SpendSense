create table import_jobs (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    ingestion_session_id uuid references ingestion_sessions (id) on delete set null,
    account_id uuid references accounts (id) on delete set null,
    source varchar(48) not null default 'CSV',
    status varchar(48) not null default 'STARTED',
    original_filename varchar(260) not null,
    file_checksum varchar(128) not null,
    idempotency_key varchar(220),
    mapping_json text not null,
    summary_json text,
    records_seen integer not null default 0,
    records_imported integer not null default 0,
    records_duplicate integer not null default 0,
    records_failed integer not null default 0,
    started_at timestamptz not null default now(),
    completed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_import_jobs_counts check (
        records_seen >= 0
        and records_imported >= 0
        and records_duplicate >= 0
        and records_failed >= 0
    )
);

create table import_failures (
    id uuid primary key,
    import_job_id uuid not null references import_jobs (id) on delete cascade,
    row_number integer not null,
    error_code varchar(80) not null,
    message varchar(500) not null,
    severity varchar(32) not null default 'ERROR',
    raw_row_json text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table import_mappings (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    source varchar(48) not null default 'CSV',
    name varchar(160) not null,
    file_signature varchar(160) not null,
    mapping_json text not null,
    last_used_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_import_jobs_user_started
    on import_jobs (user_profile_id, started_at desc);
create index idx_import_jobs_user_status
    on import_jobs (user_profile_id, status, started_at desc);
create index idx_import_jobs_session
    on import_jobs (ingestion_session_id);
create index idx_import_failures_job_row
    on import_failures (import_job_id, row_number);
create index idx_import_mappings_user_used
    on import_mappings (user_profile_id, last_used_at desc);

create unique index ux_import_jobs_user_idempotency
    on import_jobs (user_profile_id, idempotency_key)
    where idempotency_key is not null;

create unique index ux_import_mappings_user_signature
    on import_mappings (user_profile_id, source, file_signature);

comment on table import_jobs is 'Auditable import runs for CSV now and future bank or SMS ingestion adapters.';
comment on table import_failures is 'Row-level import validation, duplicate, and normalization failures.';
comment on table import_mappings is 'Reusable user column mappings keyed by CSV header signatures.';
