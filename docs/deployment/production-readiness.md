# Staging and Production Deployment Readiness

SpendSense phase 15 prepares managed staging, production integrations, operational safeguards, and release confidence without binding the product to one provider.

## Frontend

- Vercel config: `vercel.json`
- Docker image: `apps/web/Dockerfile`
- Required public env: `NEXT_PUBLIC_SITE_URL`, `NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`
- Required managed release env: `NEXT_PUBLIC_APP_ENV`, `NEXT_PUBLIC_APP_VERSION`, `NEXT_PUBLIC_RELEASE_COMMIT`
- Operational env: `NEXT_PUBLIC_MAINTENANCE_MODE`, `NEXT_PUBLIC_DEGRADED_MODE`, `NEXT_PUBLIC_FEATURE_FLAGS`
- Optional observability env: `NEXT_PUBLIC_SENTRY_DSN`, `SENTRY_AUTH_TOKEN`, `SENTRY_ORG`, `SENTRY_FRONTEND_PROJECT`
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
- Health checks: `/api/v1/health/live`, `/api/v1/health/ready`, `/api/v1/health/heartbeat`, `/api/v1/health/version`, `/actuator/health/liveness`, `/actuator/health/readiness`
- Migrations: Flyway runs on startup with `ddl-auto=validate`; keep `FLYWAY_BASELINE_ON_MIGRATE=false` unless manually recovering a known database.
- Maintenance mode blocks write requests with `503` and `Retry-After` while health and read traffic remain available.
- Release metadata is logged at startup and returned by `/api/v1/health/version`.

## Deployment Verification

```powershell
./scripts/deployment/validate-env.ps1 -Environment staging -EnvFile .env.staging
./scripts/deployment/smoke-test.ps1 -WebBaseUrl https://staging.spendsense.example -ApiBaseUrl https://api-staging.spendsense.example
```

Use `validate-production-env.ps1` or `validate-env.ps1 -Environment production` before production cutover.

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
