# Staging and Production Deployment Readiness

SpendSense phase 16 prepares managed staging, production integrations, operational safeguards, deployment rehearsal, incident drill evidence, and release confidence without binding the product to one provider.

## Frontend

- Vercel config: `vercel.json`
- Docker image: `apps/web/Dockerfile`
- Required public env: `NEXT_PUBLIC_SITE_URL`, `NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`
- Required managed release env: `NEXT_PUBLIC_APP_ENV`, `NEXT_PUBLIC_APP_VERSION`, `NEXT_PUBLIC_RELEASE_COMMIT`
- Operational env: `NEXT_PUBLIC_MAINTENANCE_MODE`, `NEXT_PUBLIC_DEGRADED_MODE`, `NEXT_PUBLIC_FEATURE_FLAGS`
- Optional observability env: `NEXT_PUBLIC_SENTRY_DSN`, `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_FRONTEND_PROJECT`
- Staging observability must use staging Sentry projects and staging DSNs, never production project DSNs.
- Budget gates: `npm --prefix apps/web run budget:bundle` and `npm --prefix apps/web run lighthouse:ci`
- Staging builds must use HTTPS managed URLs and staging Supabase credentials. Production builds must use production Supabase credentials.

For Lighthouse CI dashboard runs, build with:

```bash
NEXT_PUBLIC_API_BASE_URL=http://127.0.0.1:3000 \
NEXT_PUBLIC_ENABLE_E2E_AUTH_BYPASS=1 \
SPENDSENSE_ENABLE_CI_MOCK_API=1 \
npm --prefix apps/web run build
```

## Backend

- Docker image: `apps/api/Dockerfile`
- Provider starters: `render.yaml`, `railway.json`, `fly.toml`, `fly.staging.toml`
- Required profiles: `SPRING_PROFILES_ACTIVE=staging` for staging, `SPRING_PROFILES_ACTIVE=production` for production
- Required managed secrets: database credentials, Supabase JWT verification, production CORS origin
- Health checks: `/api/v1/health/live`, `/api/v1/health/ready`, `/api/v1/health/deployment`, `/api/v1/health/dependencies`, `/api/v1/health/heartbeat`, `/api/v1/health/version`, `/actuator/health/liveness`, `/actuator/health/readiness`
- Migrations: Flyway runs on startup with `ddl-auto=validate`; keep `FLYWAY_BASELINE_ON_MIGRATE=false` unless manually recovering a known database.
- Maintenance mode blocks write requests with `503` and `Retry-After` while health and read traffic remain available.
- Release metadata is logged at startup, returned by `/api/v1/health/version`, and recorded in `operational_trace_events`.
- Admin operations exposes `/api/v1/admin/operations/trace-events` for startup validation, retry exhaustion, operator retry, shutdown release, and drill evidence.

## Deployment Verification

```powershell
./scripts/deployment/validate-env.ps1 -Environment staging -EnvFile .env.staging
./scripts/deployment/smoke-test.ps1 -WebBaseUrl https://staging.spendsense.example -ApiBaseUrl https://api-staging.spendsense.example
./scripts/deployment/incident-drill-check.ps1 -ApiBaseUrl https://api-staging.spendsense.example -ExpectedStatus UP -ExpectedEnvironment staging -ExpectedCommit <deployed-git-sha> -RequireQueueMonitoring
```

Use `validate-production-env.ps1` or `validate-env.ps1 -Environment production` before production cutover.

## Staging Rehearsal

- Create a separate Supabase staging project and apply migrations against only the staging Postgres database.
- Configure Vercel preview/staging variables from `.env.staging.example`; keep all staging URLs on staging domains.
- Configure backend staging secrets in the managed provider, including database credentials, Sentry DSN, Supabase JWKS, webhook secrets, and alert escalation email.
- Verify HTTPS for web and API domains before auth testing.
- Run `staging-deployment.yml` from `main`; it validates environment shape, builds web/API, deploys when provider hooks are configured, then runs smoke and incident drill checks.
- Confirm Supabase auth manually with a staging-only test account; do not reuse production users, production database URLs, or production Sentry projects.

## Incident Drill Matrix

- Provider outage simulation: force provider send failures in staging, confirm provider alerts, incident grouping, and runbook links.
- Failed worker simulation: stop the worker or let a lock expire, confirm stale heartbeat/expired lock alerts and queue recovery traces.
- Queue backlog simulation: enqueue delayed staging jobs or pause workers, confirm backlog alert thresholds and dashboard lag.
- Webhook replay simulation: replay the same provider payload, confirm duplicate webhook audit and delivery timeline behavior.
- Failed deployment rollback: deploy a bad staging build, activate maintenance mode if writes are unsafe, roll back to the previous artifact, then run `incident-drill-check.ps1` with the expected commit.
- Retry exhaustion scenario: allow a staging job to exhaust retries, confirm dead-letter creation, `retry_exhausted` trace event, and admin retry audit.

## Rollback Safety

- Keep staging and production Supabase projects separate.
- Keep `SPENDSENSE_RELEASE_COMMIT` and `NEXT_PUBLIC_RELEASE_COMMIT` pinned to the deployed commit.
- Keep `FLYWAY_BASELINE_ON_MIGRATE=false` during normal releases.
- Prefer toggling `SPENDSENSE_MAINTENANCE_MODE=true` before rollback when write safety matters.
- Verify `/api/v1/health/ready` and `/api/v1/health/version` after rollback before restoring traffic.

## Checklist

- Production env has no localhost URLs or default passwords.
- `WEB_ORIGIN` exactly matches the frontend origin.
- Supabase JWT verification uses `SUPABASE_JWKS_URI` or a managed `SUPABASE_JWT_SECRET`.
- Webhook signature secrets are configured before `SPENDSENSE_WEBHOOK_REQUIRE_SIGNATURE=true`.
- Readiness returns `UP` before traffic cutover.
- CI passes lint, typecheck, frontend build, backend build, Playwright, bundle budget, and Lighthouse CI.
- Sentry DSN is configured only in managed environments.
- Uptime monitoring targets `/api/v1/health/ready` and the frontend `/dashboard` route.
- Release monitoring tags Sentry events with environment and release.
- Operational traces record startup validation, release boot, worker shutdown release, retry scheduling, retry exhaustion, and operator retries.
