# Production Deployment Readiness

SpendSense phase 14 prepares deployment foundations without binding the product to one provider.

## Frontend

- Vercel config: `vercel.json`
- Docker image: `apps/web/Dockerfile`
- Required public env: `NEXT_PUBLIC_SITE_URL`, `NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`
- Optional observability env: `NEXT_PUBLIC_SENTRY_DSN`, `NEXT_PUBLIC_APP_VERSION`
- Budget gates: `npm --prefix apps/web run budget:bundle` and `npm --prefix apps/web run lighthouse:ci`

For Lighthouse CI dashboard runs, build with:

```bash
NEXT_PUBLIC_API_BASE_URL=http://127.0.0.1:3000 \
NEXT_PUBLIC_ENABLE_E2E_AUTH_BYPASS=1 \
SPENDSENSE_ENABLE_CI_MOCK_API=1 \
npm --prefix apps/web run build
```

## Backend

- Docker image: `apps/api/Dockerfile`
- Provider starters: `render.yaml`, `railway.json`, `fly.toml`
- Required profile: `SPRING_PROFILES_ACTIVE=production`
- Required managed secrets: database credentials, Supabase JWT verification, production CORS origin
- Health checks: `/api/v1/health/live`, `/api/v1/health/ready`, `/actuator/health/liveness`, `/actuator/health/readiness`
- Migrations: Flyway runs on startup with `ddl-auto=validate`; keep `FLYWAY_BASELINE_ON_MIGRATE=false` unless manually recovering a known database.

## Checklist

- Production env has no localhost URLs or default passwords.
- `WEB_ORIGIN` exactly matches the frontend origin.
- Supabase JWT verification uses `SUPABASE_JWKS_URI` or a managed `SUPABASE_JWT_SECRET`.
- Webhook signature secrets are configured before `SPENDSENSE_WEBHOOK_REQUIRE_SIGNATURE=true`.
- Readiness returns `UP` before traffic cutover.
- CI passes lint, typecheck, frontend build, backend build, Playwright, bundle budget, and Lighthouse CI.
- Sentry DSN is configured only in managed environments.
- Uptime monitoring targets `/api/v1/health/ready` and the frontend `/dashboard` route.
