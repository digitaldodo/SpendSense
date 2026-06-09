# Environment Setup

Environment files are split by boundary:

- `.env.example` for Docker Compose and root infrastructure
- `apps/web/.env.example` for public browser-safe variables
- `apps/api/.env.example` for backend runtime variables

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
- `production` uses managed secrets, managed MongoDB, strict logging, and locked-down CORS.

Phase 1 prepares the shape but does not productionize deployment.
