# SpendSense

SpendSense is a financial intelligence platform foundation for an AI-powered financial mentor, debt prevention system, safe EMI advisor, and future wealth simulator.

SpendSense is built incrementally. The current foundation includes protected web flows, financial import/dashboard surfaces, delivery operations, production health checks, and CI/CD quality gates. It still intentionally excludes AI mentor/chat, bank APIs, SMS parsing, predictive ML, EMI simulation, and a microservices split.

## Repository Strategy

SpendSense uses a monorepo-ready layout because the frontend, backend, shared contracts, and future mobile application will evolve around the same product language and API contracts.

Separate repositories can work once teams and deployment cadence diverge, but they add coordination overhead too early. This repository keeps product foundations together while preserving clear app boundaries:

```text
apps/
  web/      Next.js App Router frontend
  api/      Spring Boot API
packages/
  shared/   Shared tokens and future cross-platform contracts
docs/
  architecture/
  development/
  environment/
```

Future React Native readiness comes from keeping shared primitives platform-neutral in `packages/shared`. Web-only components stay in `apps/web/src/components`, API-only concerns stay in `apps/api`, and DTO/API contract work can later move into dedicated shared packages.

## Setup

Prerequisites:

- Node.js 22+
- npm 10+
- Java 21
- Docker Desktop

Install dependencies:

```bash
npm install
```

Start local infrastructure:

```bash
docker compose up -d
```

Run the frontend:

```bash
npm run dev:web
```

Run the backend:

```bash
npm run dev:api
```

Frontend: `http://localhost:3000`

Backend health: `http://localhost:8080/api/v1/health`

Swagger UI: `http://localhost:8080/swagger-ui`

## Tooling

- Next.js App Router, TypeScript, Tailwind CSS, and Shadcn/UI provide a typed, token-driven frontend foundation.
- TanStack Query centralizes server-state behavior before product APIs arrive.
- React Hook Form and Zod establish validation-first form composition.
- Spring Boot, PostgreSQL, Flyway, validation, security, actuator, and Springdoc provide the backend foundation.
- Docker Compose supplies local PostgreSQL without production deployment assumptions.
- Prettier, ESLint, Husky, and lint-staged keep formatting and commit hygiene consistent.
- GitHub Actions, Playwright, bundle budgets, and Lighthouse CI provide production-readiness gates.

## Development Workflow

1. Copy `.env.example`, `apps/web/.env.example`, and `apps/api/.env.example` into local `.env` files when needed.
2. Start PostgreSQL with `npm run dev:infra`.
3. Run API and web in separate terminals.
4. Use `npm run lint`, `npm run typecheck`, `npm run build`, `npm run budget:bundle`, `npm run test:e2e`, and `npm run lighthouse:ci` before handing off production-facing changes.

## Production Foundation

- Frontend deployment: Vercel config and `apps/web/Dockerfile`
- Backend deployment: Render, Railway, Fly, and `apps/api/Dockerfile`
- Health checks: `GET /api/v1/health/live` and `GET /api/v1/health/ready`
- Deployment checklist: `docs/deployment/production-readiness.md`
- Production env example: `.env.production.example`

## Git Discipline

Recommended branch:

```bash
git checkout -b feat/phase-1-foundation
```

Recommended commit:

```bash
feat(core): scaffold frontend backend foundation
```

Recommended push:

```bash
git push -u origin feat/phase-1-foundation
```
