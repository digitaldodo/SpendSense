create table notifications (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    notification_type varchar(64) not null,
    severity varchar(24) not null default 'INFO',
    title varchar(160) not null,
    body varchar(520) not null,
    action_label varchar(80),
    action_url varchar(240),
    source_type varchar(64),
    source_id varchar(120),
    delivery_channel varchar(32) not null default 'IN_APP',
    lifecycle_status varchar(32) not null default 'ACTIVE',
    priority integer not null default 3,
    dedupe_key varchar(180),
    payload_json text,
    scheduled_for timestamptz,
    delivered_at timestamptz,
    read_at timestamptz,
    dismissed_at timestamptz,
    expires_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_notifications_priority check (priority >= 1 and priority <= 5)
);

create table notification_preferences (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    in_app_enabled boolean not null default true,
    budget_warnings_enabled boolean not null default true,
    recurring_reminders_enabled boolean not null default true,
    report_ready_enabled boolean not null default true,
    savings_nudges_enabled boolean not null default true,
    spending_increase_enabled boolean not null default true,
    weekly_digest_enabled boolean not null default false,
    monthly_report_enabled boolean not null default false,
    timezone varchar(64) not null default 'Asia/Kolkata',
    quiet_hours_start time,
    quiet_hours_end time,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table scheduled_reports (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    report_type varchar(48) not null,
    format varchar(16) not null,
    cadence varchar(24) not null,
    timezone varchar(64) not null default 'Asia/Kolkata',
    delivery_channel varchar(32) not null default 'IN_APP',
    next_run_at timestamptz not null,
    last_run_at timestamptz,
    active boolean not null default true,
    metadata_json text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table report_delivery_logs (
    id uuid primary key,
    scheduled_report_id uuid references scheduled_reports (id) on delete set null,
    generated_report_id uuid references generated_reports (id) on delete set null,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    delivery_channel varchar(32) not null,
    status varchar(32) not null,
    attempted_at timestamptz not null default now(),
    delivered_at timestamptz,
    error_message varchar(520),
    metadata_json text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index ux_notifications_user_dedupe
    on notifications (user_profile_id, dedupe_key)
    where dedupe_key is not null;
create index idx_notifications_user_state_created
    on notifications (user_profile_id, lifecycle_status, created_at desc);
create index idx_notifications_user_unread
    on notifications (user_profile_id, read_at, created_at desc)
    where lifecycle_status = 'ACTIVE';
create unique index ux_notification_preferences_user
    on notification_preferences (user_profile_id);
create index idx_scheduled_reports_due
    on scheduled_reports (active, next_run_at);
create index idx_scheduled_reports_user
    on scheduled_reports (user_profile_id, active, next_run_at);
create index idx_report_delivery_logs_user_attempted
    on report_delivery_logs (user_profile_id, attempted_at desc);

comment on table notifications is 'Auditable in-app notification lifecycle with source, read, dismissed, and future delivery channel fields.';
comment on table notification_preferences is 'User-controlled deterministic notification and report preference switches.';
comment on table scheduled_reports is 'Timezone-aware scheduled report definitions prepared for future push and email delivery.';
comment on table report_delivery_logs is 'Append-only delivery attempts for scheduled exports and future channels.';
