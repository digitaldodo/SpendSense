create table recurring_transactions (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    category_id uuid references categories (id) on delete set null,
    merchant_normalized varchar(180) not null,
    merchant_name varchar(180) not null,
    amount numeric(16, 2) not null,
    currency varchar(3) not null default 'INR',
    cadence varchar(32) not null,
    occurrence_count integer not null,
    first_seen_on date not null,
    last_seen_on date not null,
    next_expected_on date,
    confidence numeric(5, 2) not null default 0,
    state varchar(32) not null default 'ACTIVE',
    metadata_json text,
    detected_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_recurring_occurrence_count check (occurrence_count >= 2),
    constraint chk_recurring_confidence check (confidence >= 0 and confidence <= 100)
);

create table insight_snapshots (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    snapshot_type varchar(48) not null,
    period_start date not null,
    period_end date not null,
    payload_json text not null,
    generated_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table generated_reports (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    report_type varchar(48) not null,
    format varchar(16) not null,
    period_start date not null,
    period_end date not null,
    status varchar(32) not null default 'GENERATED',
    file_name varchar(220) not null,
    metadata_json text,
    generated_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table budget_rollovers (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    budget_id uuid references budgets (id) on delete set null,
    category_id uuid references categories (id) on delete set null,
    source_period_start date not null,
    source_period_end date not null,
    target_period_start date not null,
    target_period_end date not null,
    original_amount numeric(16, 2) not null,
    spent_amount numeric(16, 2) not null default 0,
    rollover_amount numeric(16, 2) not null default 0,
    state varchar(32) not null default 'MATERIALIZED',
    metadata_json text,
    materialized_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index ux_recurring_user_merchant_amount
    on recurring_transactions (user_profile_id, merchant_normalized, amount, cadence);
create index idx_recurring_user_state
    on recurring_transactions (user_profile_id, state, next_expected_on);
create index idx_insight_snapshots_user_period
    on insight_snapshots (user_profile_id, snapshot_type, period_start desc);
create index idx_generated_reports_user_generated
    on generated_reports (user_profile_id, generated_at desc);
create unique index ux_budget_rollovers_budget_target
    on budget_rollovers (user_profile_id, budget_id, target_period_start);
create index idx_budget_rollovers_user_target
    on budget_rollovers (user_profile_id, target_period_start desc);

comment on table recurring_transactions is 'Deterministically detected recurring debit patterns from posted user transactions.';
comment on table insight_snapshots is 'Historical deterministic insight payloads for auditability and future AI compatibility.';
comment on table generated_reports is 'Audit log for generated CSV and PDF financial reports.';
comment on table budget_rollovers is 'Materialized rollover calculations for monthly budgets with rollover enabled.';
