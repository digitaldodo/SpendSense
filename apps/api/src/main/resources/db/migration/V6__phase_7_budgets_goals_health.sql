create table budgets (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    category_id uuid not null references categories (id) on delete restrict,
    name varchar(160) not null,
    amount numeric(16, 2) not null,
    currency varchar(3) not null default 'INR',
    period varchar(24) not null default 'MONTHLY',
    starts_on date not null,
    ends_on date,
    rollover_enabled boolean not null default false,
    active boolean not null default true,
    metadata_json text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_budgets_amount_positive check (amount > 0),
    constraint chk_budgets_dates check (ends_on is null or ends_on >= starts_on)
);

create table budget_history (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    budget_id uuid references budgets (id) on delete set null,
    category_id uuid references categories (id) on delete set null,
    action varchar(48) not null,
    previous_amount numeric(16, 2),
    new_amount numeric(16, 2),
    previous_name varchar(160),
    new_name varchar(160),
    previous_active boolean,
    new_active boolean,
    period_start date,
    period_end date,
    snapshot_json text,
    reason varchar(240),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table savings_goals (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    name varchar(160) not null,
    target_amount numeric(16, 2) not null,
    current_amount numeric(16, 2) not null default 0,
    currency varchar(3) not null default 'INR',
    target_date date,
    status varchar(32) not null default 'ACTIVE',
    color_token varchar(48) not null default 'green',
    icon_name varchar(64) not null default 'target',
    metadata_json text,
    completed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_savings_goals_target_positive check (target_amount > 0),
    constraint chk_savings_goals_current_non_negative check (current_amount >= 0)
);

create table goal_contributions (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    savings_goal_id uuid not null references savings_goals (id) on delete cascade,
    amount numeric(16, 2) not null,
    contributed_on date not null,
    source varchar(48) not null default 'MANUAL',
    note varchar(240),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_goal_contributions_amount_positive check (amount > 0)
);

create table custom_categories (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    category_id uuid references categories (id) on delete set null,
    action varchar(48) not null,
    before_json text,
    after_json text,
    reason varchar(240),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index ux_budgets_user_category_monthly_active
    on budgets (user_profile_id, category_id, starts_on)
    where active = true and period = 'MONTHLY';

create index idx_budgets_user_active
    on budgets (user_profile_id, active, starts_on desc);
create index idx_budgets_user_category
    on budgets (user_profile_id, category_id, starts_on desc);
create index idx_budget_history_user_created
    on budget_history (user_profile_id, created_at desc);
create index idx_budget_history_budget_created
    on budget_history (budget_id, created_at desc);

create index idx_savings_goals_user_status
    on savings_goals (user_profile_id, status, target_date);
create index idx_goal_contributions_goal_date
    on goal_contributions (savings_goal_id, contributed_on desc);
create index idx_goal_contributions_user_date
    on goal_contributions (user_profile_id, contributed_on desc);

create index idx_custom_categories_user_created
    on custom_categories (user_profile_id, created_at desc);
create index idx_custom_categories_category_created
    on custom_categories (category_id, created_at desc);

comment on table budgets is 'User category budget plans with monthly period foundation and future rollover compatibility.';
comment on table budget_history is 'Auditable budget lifecycle snapshots for edits, deactivations, and future analytics.';
comment on table savings_goals is 'User savings targets advanced only by explicit goal contributions.';
comment on table goal_contributions is 'Auditable manual contribution ledger for savings goal progress calculations.';
comment on table custom_categories is 'Audit trail for custom category creation, rename, style updates, and merges.';
