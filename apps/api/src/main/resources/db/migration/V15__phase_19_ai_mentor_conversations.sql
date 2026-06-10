create table ai_conversations (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    title varchar(160) not null,
    status varchar(32) not null default 'ACTIVE',
    context_scope varchar(64) not null default 'FINANCIAL_WORKSPACE',
    memory_summary text,
    last_message_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_ai_conversations_status check (status in ('ACTIVE', 'ARCHIVED'))
);

create table ai_messages (
    id uuid primary key,
    conversation_id uuid not null references ai_conversations (id) on delete cascade,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    role varchar(24) not null,
    intent varchar(64) not null,
    content text not null,
    structured_json text not null default '{}',
    grounded_context_json text not null default '{}',
    safety_flags text not null default '[]',
    provider varchar(64),
    model varchar(96),
    prompt_tokens integer not null default 0,
    completion_tokens integer not null default 0,
    latency_ms integer not null default 0,
    created_at timestamptz not null default now(),
    constraint chk_ai_messages_role check (role in ('USER', 'ASSISTANT', 'SYSTEM')),
    constraint chk_ai_messages_tokens check (prompt_tokens >= 0 and completion_tokens >= 0)
);

create table ai_usage_logs (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    conversation_id uuid references ai_conversations (id) on delete set null,
    message_id uuid references ai_messages (id) on delete set null,
    provider varchar(64) not null,
    model varchar(96) not null,
    prompt_tokens integer not null default 0,
    completion_tokens integer not null default 0,
    total_tokens integer not null default 0,
    estimated_cost_minor numeric(12, 4) not null default 0,
    currency varchar(3) not null default 'INR',
    status varchar(32) not null,
    safety_outcome varchar(64) not null,
    latency_ms integer not null default 0,
    metadata_json text not null default '{}',
    created_at timestamptz not null default now(),
    constraint chk_ai_usage_tokens check (prompt_tokens >= 0 and completion_tokens >= 0 and total_tokens >= 0)
);

create table ai_feedback (
    id uuid primary key,
    user_profile_id uuid not null references user_profiles (id) on delete cascade,
    conversation_id uuid not null references ai_conversations (id) on delete cascade,
    message_id uuid not null references ai_messages (id) on delete cascade,
    rating smallint,
    feedback_type varchar(48) not null,
    comment text,
    metadata_json text not null default '{}',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint chk_ai_feedback_rating check (rating is null or rating between 1 and 5)
);

create index idx_ai_conversations_user_last_message
    on ai_conversations (user_profile_id, last_message_at desc);
create index idx_ai_messages_conversation_created
    on ai_messages (conversation_id, created_at asc);
create index idx_ai_messages_user_intent_created
    on ai_messages (user_profile_id, intent, created_at desc);
create index idx_ai_usage_user_created
    on ai_usage_logs (user_profile_id, created_at desc);
create index idx_ai_feedback_message
    on ai_feedback (message_id, created_at desc);

comment on table ai_conversations is 'Auditable AI mentor conversation sessions with future memory and moderation readiness.';
comment on table ai_messages is 'Sanitized AI mentor messages with structured responses and grounded context snapshots.';
comment on table ai_usage_logs is 'Provider, model, latency, token, and cost tracking foundation for AI calls.';
comment on table ai_feedback is 'User feedback and moderation readiness for AI mentor responses.';
