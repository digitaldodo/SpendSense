create table smart_actions (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    deterministic_key varchar(160) not null,
    action_type varchar(64) not null,
    category varchar(64) not null,
    status varchar(32) not null default 'OPEN',
    priority integer not null default 50,
    title varchar(180) not null,
    body text not null,
    explanation text not null,
    impact_amount numeric(16, 2) not null default 0,
    impact_percent numeric(8, 2) not null default 0,
    currency varchar(3) not null default 'INR',
    source_type varchar(64) not null,
    source_id varchar(96),
    recommendation_json text not null default '{}',
    due_on date,
    snoozed_until timestamptz,
    completed_at timestamptz,
    dismissed_at timestamptz,
    generated_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_smart_actions_user_key unique (user_profile_id, deterministic_key),
    constraint chk_smart_actions_status check (status in ('OPEN', 'SNOOZED', 'COMPLETED', 'DISMISSED')),
    constraint chk_smart_actions_priority check (priority between 0 and 100)
);

create table action_history (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    smart_action_id uuid not null references smart_actions (id) on delete cascade,
    event_type varchar(48) not null,
    previous_status varchar(32),
    new_status varchar(32) not null,
    reason text,
    metadata_json text not null default '{}',
    created_at timestamptz not null default now()
);

create table financial_streaks (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    streak_key varchar(96) not null,
    label varchar(160) not null,
    current_count integer not null default 0,
    best_count integer not null default 0,
    unit varchar(32) not null default 'days',
    state varchar(32) not null default 'STEADY',
    last_qualified_on date,
    evaluation_json text not null default '{}',
    calculated_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_financial_streaks_user_key unique (user_profile_id, streak_key),
    constraint chk_financial_streak_counts check (current_count >= 0 and best_count >= 0)
);

create table engagement_events (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    event_type varchar(64) not null,
    source_type varchar(64) not null,
    source_id uuid,
    metadata_json text not null default '{}',
    occurred_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

create table weekly_summaries (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    week_start date not null,
    week_end date not null,
    status varchar(32) not null default 'GENERATED',
    headline varchar(220) not null,
    wins_json text not null default '[]',
    focus_json text not null default '[]',
    summary_json text not null default '{}',
    generated_at timestamptz not null default now(),
    completed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_weekly_summaries_user_week unique (user_profile_id, week_start),
    constraint chk_weekly_summaries_status check (status in ('GENERATED', 'COMPLETED'))
);

create index idx_smart_actions_user_status_priority
    on smart_actions (user_profile_id, status, priority desc, generated_at desc);
create index idx_smart_actions_user_type_generated
    on smart_actions (user_profile_id, action_type, generated_at desc);
create index idx_action_history_action_created
    on action_history (smart_action_id, created_at desc);
create index idx_action_history_user_event_created
    on action_history (user_profile_id, event_type, created_at desc);
create index idx_financial_streaks_user_state
    on financial_streaks (user_profile_id, state, calculated_at desc);
create index idx_engagement_events_user_occurred
    on engagement_events (user_profile_id, occurred_at desc);
create index idx_engagement_events_user_type
    on engagement_events (user_profile_id, event_type, occurred_at desc);
create index idx_weekly_summaries_user_generated
    on weekly_summaries (user_profile_id, generated_at desc);

comment on table smart_actions is 'Deterministic, explainable financial coaching actions generated from SpendSense ledger, budget, subscription, and goal data.';
comment on table action_history is 'Immutable status transition audit log for smart actions, including completion, dismissal, snooze, and regeneration events.';
comment on table financial_streaks is 'Habit reinforcement snapshots calculated from financial behavior without pressure-oriented gamification.';
comment on table engagement_events is 'Scalable engagement analytics foundation for views, check-ins, action state changes, and future product analytics.';
comment on table weekly_summaries is 'Auditable weekly financial recap snapshots built from deterministic summaries and completed by user check-in.';
