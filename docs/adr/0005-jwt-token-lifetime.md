# 0005 — JWT token lifetime

## Status
Accepted — 2026-04

## Context
Audit PR-4 flagged `ACCESS_TOKEN_DURABILITY = 1000 * 60 * 30 * 24 * 7` —
3.5 days — and `REFRESH_TOKEN_DURABILITY = 7 days`. The literal was almost
certainly a typo (the formula reads like "1000 * 60 * 60 * 24 * 7" with `60`
replaced by `30`). Industry standard for access tokens is 5–60 min.

## Decision
- Access token: **15 minutes** (`1000L * 60 * 15`).
- Refresh token: **14 days** (`1000L * 60 * 60 * 24 * 14`).
- Refresh tokens are stored in `auth-service`'s Redis (`AuthRefreshTokenRedisKey`)
  and can be revoked server-side.
- JWT signing key reads from `${JWT_SECRET}`; a 32-char dev default lives in
  `.env.example`. Rotation procedure is documented in ADR 0006.

## Consequences
- Shorter access tokens reduce blast radius on token leak.
- Clients must implement refresh flow; can no longer cache an access token
  for days.
- Server-side revocation only affects refresh tokens — access tokens remain
  valid until expiry. Acceptable given the 15-min ceiling.
