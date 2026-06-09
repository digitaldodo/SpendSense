alter table user_profiles
    add column onboarding_completed boolean not null default false,
    add column onboarding_completed_at timestamptz;

create table onboarding_progress (
    id uuid primary key,
    user_profile_id uuid not null unique references user_profiles (id) on delete cascade,
    current_step integer not null default 0,
    completed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_onboarding_progress_current_step check (current_step between 0 and 7)
);

create table onboarding_completed_steps (
    onboarding_progress_id uuid not null references onboarding_progress (id) on delete cascade,
    step varchar(64) not null,
    primary key (onboarding_progress_id, step)
);

create table financial_preferences (
    id uuid primary key,
    user_profile_id uuid not null unique references user_profiles (id) on delete cascade,
    salary_range varchar(64),
    employment_type varchar(64),
    monthly_fixed_expenses numeric(14, 2),
    risk_comfort varchar(64),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_financial_preferences_fixed_expenses check (
        monthly_fixed_expenses is null or monthly_fixed_expenses >= 0
    )
);

create table financial_preference_goals (
    financial_preferences_id uuid not null references financial_preferences (id) on delete cascade,
    goal varchar(64) not null,
    primary key (financial_preferences_id, goal)
);

create table financial_preference_spending_habits (
    financial_preferences_id uuid not null references financial_preferences (id) on delete cascade,
    spending_habit varchar(64) not null,
    primary key (financial_preferences_id, spending_habit)
);

create index idx_onboarding_progress_user_profile on onboarding_progress (user_profile_id);
create index idx_financial_preferences_user_profile on financial_preferences (user_profile_id);
create index idx_financial_preference_goals_goal on financial_preference_goals (goal);
create index idx_financial_preference_spending_habits_habit
    on financial_preference_spending_habits (spending_habit);

comment on table onboarding_progress is 'Per-user onboarding resume and completion metadata.';
comment on table financial_preferences is 'Structured onboarding financial preferences for future analytics features.';
