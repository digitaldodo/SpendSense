create table user_profiles (
    id uuid primary key,
    supabase_user_id uuid not null unique,
    email varchar(320) not null,
    display_name varchar(160),
    last_seen_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_user_profiles_email on user_profiles (email);

comment on table user_profiles is 'Application-owned user profile sync table keyed to Supabase Auth users.';
comment on column user_profiles.supabase_user_id is 'References auth.users.id in Supabase Auth without issuing application JWTs.';
