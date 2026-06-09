# SpendSense Web

The web app is a Next.js App Router application using TypeScript, Tailwind CSS 4, Shadcn/UI, TanStack Query, React Hook Form, Zod, next-themes, and Framer Motion-ready dependencies.

## Structure

```text
src/
  app/          Route groups, metadata, root layout
  components/   Reusable UI and layout primitives
  config/       Environment parsing
  features/     Future vertical product features
  hooks/        Shared React hooks
  lib/          Low-level utilities
  providers/    App-level providers
  services/     API and integration clients
  shared/       Cross-feature helpers
  styles/       Token exports and styling support
  types/        Shared TypeScript contracts
```

## Design System

Design tokens live in `src/app/globals.css` as CSS variables and are exposed to Tailwind through `@theme inline`. Components should use semantic utilities such as `bg-background`, `text-muted-foreground`, `border-border`, `shadow-raised`, and layout helpers like `container-app`.

Do not add random inline color, spacing, radius, shadow, or z-index values. Add semantic tokens first when the product language needs a new primitive.

## Route Groups

- `(public)` for marketing or public education surfaces.
- `(auth)` for Phase 2 authentication UI.
- `(dashboard)` for protected product areas.

The groups currently contain structural shells only.

## Commands

```bash
npm run dev
npm run lint
npm run typecheck
npm run build
```
