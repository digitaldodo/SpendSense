# Environment Setup

Environment files are split by boundary:

- `.env.example` for Docker Compose and root infrastructure
- `apps/web/.env.example` for public browser-safe variables
- `apps/api/.env.example` for backend runtime variables
- `.env.production.example` for managed deployment environments

Never commit secrets. Values prefixed with `NEXT_PUBLIC_` are shipped to the browser and must not contain sensitive information.

## Local

```bash
cp .env.example .env
cp apps/web/.env.example apps/web/.env.local
cp apps/api/.env.example apps/api/.env
docker compose up -d
```

## Environments

- `local` is for individual developer machines.
- `development` is for shared integration.
- `staging` mirrors production configuration more closely.
- `production` uses managed secrets, Supabase Postgres, strict logging, locked-down CORS, health probes, and startup validation.

## Production Readiness

Production deployments should set `SPRING_PROFILES_ACTIVE=production`, `NEXT_PUBLIC_APP_ENV=production`, managed Supabase database credentials, `PUBLIC_BASE_URL`, `WEB_ORIGIN`, and Supabase JWT verification values. The API fails startup when production points at localhost, default passwords, missing Supabase JWT verification, or wildcard CORS.

Before cutting over:

```powershell
pwsh scripts/deployment/validate-production-env.ps1 -EnvFile .env.production
npm run lint
npm run typecheck
npm run build:web
npm run budget:bundle
cd apps/api; ./gradlew build
```

After deployment:

```powershell
pwsh scripts/deployment/check-health.ps1 -ApiBaseUrl https://api.spendsense.example
```
