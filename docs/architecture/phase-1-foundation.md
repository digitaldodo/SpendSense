# Phase 1 Foundation

## Scope

Phase 1 establishes repository scaffolding, design tokens, frontend shell, backend skeleton, local infrastructure, environment strategy, and documentation foundations.

Out of scope:

- Auth flows
- Dashboard features
- Onboarding
- Transactions
- Analytics
- AI features
- Financial calculations

## Frontend Architecture

Feature code should be organized by vertical capability under `src/features`. Shared UI primitives live in `src/components`, app-wide providers in `src/providers`, service clients in `src/services`, and validation utilities in `src/shared/validation`.

Reusable components should start generic only when they are genuinely cross-feature. Feature-specific components should stay inside their feature boundary until reuse is real.

## Design Tokens

Tailwind CSS 4 consumes CSS variables from `globals.css`. Tokens are semantic rather than brand-color literal:

- `background`, `foreground`, `card`, `popover`
- `primary`, `secondary`, `accent`, `muted`
- `success`, `warning`, `info`, `destructive`
- `border`, `input`, `ring`
- radius, elevation, z-index, duration, responsive spacing

Light and dark themes use the same semantic names so components do not branch on theme.

## Backend Architecture

The backend uses layered boundaries:

- `controller` for REST endpoints
- `dto` for request and response contracts
- `service` for application use cases
- `repository` for persistence
- `mapper` for object conversion
- `security` for JWT and role infrastructure
- `exception` for normalized failures
- `common` for reusable API envelopes

## Scalability Review

This structure supports future mobile work by keeping shared contracts outside web-only code. It supports analytics and AI integrations through isolated service clients and backend service boundaries. It supports microservice extraction later because API modules already separate transport, service, persistence, and security concerns.
