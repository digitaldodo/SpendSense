create table saved_import_mappings (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    source varchar(48) not null default 'CSV',
    name varchar(160) not null,
    file_signature varchar(160) not null,
    mapping_json text not null,
    confidence_score numeric(5, 2) not null default 0,
    use_count integer not null default 0,
    last_used_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_saved_import_mappings_confidence check (confidence_score between 0 and 100),
    constraint chk_saved_import_mappings_use_count check (use_count >= 0)
);

create table reconciliation_logs (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    import_job_id uuid references import_jobs (id) on delete set null,
    account_id uuid references accounts (id) on delete set null,
    status varchar(48) not null,
    records_seen integer not null default 0,
    records_imported integer not null default 0,
    records_duplicate integer not null default 0,
    records_failed integer not null default 0,
    opening_balance numeric(16, 2),
    closing_balance numeric(16, 2),
    imported_balance_delta numeric(16, 2) not null default 0,
    metadata_json text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table transaction_edits (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    transaction_id uuid references transactions (id) on delete set null,
    edit_type varchar(64) not null,
    before_json text,
    after_json text,
    reason varchar(240),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table import_jobs
    add column if not exists mapping_confidence_score numeric(5, 2) not null default 0,
    add column if not exists reconciliation_metadata_json text;

create index idx_saved_import_mappings_user_used
    on saved_import_mappings (user_profile_id, last_used_at desc);

create unique index ux_saved_import_mappings_user_signature
    on saved_import_mappings (user_profile_id, source, file_signature);

create index idx_reconciliation_logs_user_created
    on reconciliation_logs (user_profile_id, created_at desc);
create index idx_reconciliation_logs_import_job
    on reconciliation_logs (import_job_id);
create index idx_transaction_edits_user_created
    on transaction_edits (user_profile_id, created_at desc);
create index idx_transaction_edits_transaction
    on transaction_edits (transaction_id);

comment on table saved_import_mappings is 'Reusable user-approved import mappings with confidence metadata for CSV now and future adapters.';
comment on table reconciliation_logs is 'Auditable import/account reconciliation snapshots created during ingestion and manual correction flows.';
comment on table transaction_edits is 'Immutable audit trail for user-visible transaction changes such as category updates and exclusions.';
