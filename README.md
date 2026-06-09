# SpendSense

SpendSense is a financial intelligence platform foundation for an AI-powered financial mentor, debt prevention system, safe EMI advisor, and future wealth simulator.

Phase 1 intentionally builds infrastructure only. It does not implement authentication flows, transaction systems, dashboards, analytics, AI features, onboarding, or financial calculations.

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
- Spring Boot, MongoDB, validation, security, actuator, and Springdoc provide the backend skeleton.
- Docker Compose supplies local MongoDB without production deployment assumptions.
- Prettier, ESLint, Husky, and lint-staged keep formatting and commit hygiene consistent.

## Development Workflow

1. Copy `.env.example`, `apps/web/.env.example`, and `apps/api/.env.example` into local `.env` files when needed.
2. Start MongoDB with `npm run dev:infra`.
3. Run API and web in separate terminals.
4. Use `npm run lint`, `npm run typecheck`, and `npm run build` before handing off larger changes.

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
