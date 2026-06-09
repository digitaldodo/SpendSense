# Development Workflow

## Daily Loop

```bash
npm install
npm run dev:infra
npm run dev:web
npm run dev:api
```

Run checks before handoff:

```bash
npm run lint
npm run typecheck
npm run build
```

## Commit Convention

Use conventional commits:

```text
feat(scope): short imperative summary
fix(scope): short imperative summary
chore(scope): short imperative summary
docs(scope): short imperative summary
```

Phase 1 recommendation:

```text
feat(core): scaffold frontend backend foundation
```
