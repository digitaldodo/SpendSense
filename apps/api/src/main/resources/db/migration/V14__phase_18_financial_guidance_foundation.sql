create table financial_snapshots (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    snapshot_type varchar(48) not null,
    period_start date not null,
    period_end date not null,
    state varchar(32) not null,
    score integer,
    payload_json text not null,
    generated_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_financial_snapshots_score check (score is null or (score >= 0 and score <= 100))
);

create table affordability_scenarios (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    goal_id uuid references savings_goals (id) on delete set null,
    purchase_amount numeric(16, 2) not null,
    down_payment numeric(16, 2) not null default 0,
    financed_amount numeric(16, 2) not null,
    annual_interest_rate numeric(7, 3) not null default 0,
    tenure_months integer not null,
    monthly_emi numeric(16, 2) not null,
    safe_emi_limit numeric(16, 2) not null,
    free_cashflow_before numeric(16, 2) not null,
    free_cashflow_after numeric(16, 2) not null,
    state varchar(32) not null,
    payload_json text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_affordability_purchase_positive check (purchase_amount > 0),
    constraint chk_affordability_down_payment check (down_payment >= 0),
    constraint chk_affordability_tenure check (tenure_months > 0)
);

create table projection_history (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    projection_type varchar(48) not null,
    months_projected integer not null,
    starting_balance numeric(16, 2) not null,
    monthly_savings numeric(16, 2) not null,
    average_monthly_expense numeric(16, 2) not null,
    emergency_runway_months numeric(10, 2) not null,
    fire_style_target numeric(16, 2) not null,
    state varchar(32) not null,
    payload_json text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_projection_months check (months_projected > 0)
);

create index idx_financial_snapshots_user_type_generated
    on financial_snapshots (user_profile_id, snapshot_type, generated_at desc);
create index idx_financial_snapshots_user_period
    on financial_snapshots (user_profile_id, period_start desc, period_end desc);

create index idx_affordability_scenarios_user_created
    on affordability_scenarios (user_profile_id, created_at desc);
create index idx_affordability_scenarios_goal_created
    on affordability_scenarios (goal_id, created_at desc);

create index idx_projection_history_user_created
    on projection_history (user_profile_id, created_at desc);

comment on table financial_snapshots is 'Auditable deterministic financial health snapshots for history, comparison, and future analytics compatibility.';
comment on table affordability_scenarios is 'Auditable deterministic EMI affordability simulations created from user inputs and ledger-derived cashflow.';
comment on table projection_history is 'Deterministic future-balance and runway projection history without market-return assumptions.';
