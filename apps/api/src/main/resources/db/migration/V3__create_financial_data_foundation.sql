create table categories (
    id uuid primary key,
    user_profile_id uuid references user_profiles (id) on delete cascade,
    name varchar(120) not null,
    slug varchar(140) not null,
    color_token varchar(48) not null,
    icon_name varchar(64) not null,
    system_category boolean not null default false,
    sort_order integer not null default 100,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table accounts (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    display_name varchar(160) not null,
    institution_name varchar(160) not null,
    account_type varchar(48) not null,
    account_mask varchar(16),
    currency varchar(3) not null default 'INR',
    current_balance numeric(16, 2) not null default 0,
    available_balance numeric(16, 2),
    status varchar(48) not null default 'ACTIVE',
    source varchar(48) not null default 'MANUAL',
    source_account_id varchar(180),
    metadata_json text,
    connected_at timestamptz not null default now(),
    last_synced_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_accounts_balance_scale check (current_balance between -99999999999999.99 and 99999999999999.99)
);

create table ingestion_sessions (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    source varchar(48) not null,
    status varchar(48) not null default 'STARTED',
    started_at timestamptz not null default now(),
    completed_at timestamptz,
    records_seen integer not null default 0,
    records_imported integer not null default 0,
    records_duplicate integer not null default 0,
    error_summary text,
    metadata_json text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_ingestion_session_counts check (
        records_seen >= 0
        and records_imported >= 0
        and records_duplicate >= 0
    )
);

create table transactions (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    account_id uuid not null references accounts (id) on delete cascade,
    category_id uuid references categories (id) on delete set null,
    ingestion_session_id uuid references ingestion_sessions (id) on delete set null,
    amount numeric(16, 2) not null,
    currency varchar(3) not null default 'INR',
    direction varchar(24) not null,
    status varchar(32) not null default 'POSTED',
    occurred_at timestamptz not null,
    booked_at timestamptz,
    merchant_name varchar(180) not null,
    merchant_normalized varchar(180) not null,
    description varchar(360),
    reference varchar(220),
    source varchar(48) not null,
    source_transaction_id varchar(220),
    idempotency_key varchar(220),
    dedupe_fingerprint varchar(128) not null,
    raw_payload text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_transactions_amount_non_negative check (amount >= 0)
);

create unique index ux_categories_system_slug
    on categories (slug)
    where user_profile_id is null;

create unique index ux_categories_user_slug
    on categories (user_profile_id, slug)
    where user_profile_id is not null;

create index idx_categories_user_sort on categories (user_profile_id, sort_order, name);

create index idx_accounts_user_created on accounts (user_profile_id, created_at);
create index idx_accounts_user_status on accounts (user_profile_id, status);

create unique index ux_accounts_user_source_account
    on accounts (user_profile_id, source, source_account_id)
    where source_account_id is not null;

create index idx_ingestion_sessions_user_started
    on ingestion_sessions (user_profile_id, started_at desc);
create index idx_ingestion_sessions_user_source_status
    on ingestion_sessions (user_profile_id, source, status);

create index idx_transactions_user_occurred
    on transactions (user_profile_id, occurred_at desc);
create index idx_transactions_user_account_occurred
    on transactions (user_profile_id, account_id, occurred_at desc);
create index idx_transactions_user_category_occurred
    on transactions (user_profile_id, category_id, occurred_at desc);
create index idx_transactions_user_direction_occurred
    on transactions (user_profile_id, direction, occurred_at desc);
create index idx_transactions_user_status_occurred
    on transactions (user_profile_id, status, occurred_at desc);
create index idx_transactions_user_merchant
    on transactions (user_profile_id, merchant_normalized);
create index idx_transactions_user_dedupe
    on transactions (user_profile_id, dedupe_fingerprint);

create unique index ux_transactions_user_idempotency
    on transactions (user_profile_id, idempotency_key)
    where idempotency_key is not null;

insert into categories (
    id,
    user_profile_id,
    name,
    slug,
    color_token,
    icon_name,
    system_category,
    sort_order
) values
    ('10000000-0000-4000-8000-000000000001', null, 'Food & Dining', 'food-dining', 'mint', 'utensils', true, 10),
    ('10000000-0000-4000-8000-000000000002', null, 'Transport', 'transport', 'blue', 'car', true, 20),
    ('10000000-0000-4000-8000-000000000003', null, 'Shopping', 'shopping', 'amber', 'shopping-bag', true, 30),
    ('10000000-0000-4000-8000-000000000004', null, 'Income', 'income', 'green', 'wallet', true, 40),
    ('10000000-0000-4000-8000-000000000005', null, 'Transfers', 'transfers', 'slate', 'arrow-left-right', true, 50),
    ('10000000-0000-4000-8000-000000000006', null, 'Bills', 'bills', 'rose', 'receipt', true, 60),
    ('10000000-0000-4000-8000-000000000007', null, 'Health', 'health', 'teal', 'heart-pulse', true, 70),
    ('10000000-0000-4000-8000-000000000008', null, 'Other', 'other', 'neutral', 'circle', true, 100);

comment on table accounts is 'User-owned financial accounts prepared for manual, SMS, CSV, and bank API ingestion sources.';
comment on table transactions is 'Normalized transaction ledger with idempotency and dedupe fields for secure future ingestion.';
comment on table categories is 'System and user-visible transaction categorization labels.';
comment on table ingestion_sessions is 'Ingestion run metadata for future SMS parsing, CSV imports, and bank API syncs.';
comment on column transactions.idempotency_key is 'Stable caller-provided key for idempotent ingestion writes.';
comment on column transactions.dedupe_fingerprint is 'Normalized fingerprint for duplicate detection before import.';
comment on column transactions.merchant_normalized is 'Search and dedupe friendly merchant representation.';
