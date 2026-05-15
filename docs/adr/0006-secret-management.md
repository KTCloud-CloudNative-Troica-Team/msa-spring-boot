# 0006 — Secret management strategy

## Status
Accepted — 2026-04

## Context
Audit PR-3 found JWT signing keys, DB passwords, and Redis passwords committed
to `application.yaml` as literals. Git history therefore contains every secret
ever used.

## Decision
- Every secret in `application.yaml` reads from `${VAR_NAME:default}` where the
  default is the historical local value (so local dev still works).
- `container-compose.yaml` reads from `${VAR_NAME:-default}` (compose syntax).
- `.env.example` lists every variable with its local default and a header
  explicitly noting that real values must come from a secret manager.
- Production deployments inject secrets via Kubernetes Secrets / cloud secret
  manager — never `.env` files in production images.

## Consequences
- Any secret currently in git history (JWT signing key, DB passwords) **must
  be rotated** before going beyond local dev. The literal `dev-only-change-me-...`
  default for `JWT_SECRET` is intentionally obvious so it can't be confused
  for a real key.
- A `.env` file is git-ignored. Developers copy from `.env.example`.
- CI/CD pipelines must template a non-default `JWT_SECRET` from their secret
  store before `bootRun`.
