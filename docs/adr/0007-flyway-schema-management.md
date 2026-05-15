# 0007 — Flyway-managed schema

## Status
Accepted — 2026-04

## Context
Audit PR-2 found `ddl-auto: create` set on every service. Every restart would
DROP every table — non-viable beyond first-boot demos. No migration tool was
present.

## Decision
- Each `*-service` ships `db/migration/V1__init.sql` derived from the current
  JPA entity shape.
- `spring.jpa.hibernate.ddl-auto` defaults to `validate` (env-overridable via
  `JPA_DDL_AUTO`). On startup Hibernate verifies the live schema against the
  entities and refuses to start if they diverge.
- Flyway is the source of truth. Schema changes mean: write `Vn__name.sql`,
  update the JPA entity, restart. Hibernate's `generate-ddl: false` is
  authoritative — Hibernate never writes DDL.

## Consequences
- Local data survives restarts.
- Schema diffs are reviewable in PRs.
- Tradeoff: dev's first `bootRun` after each schema change must run Flyway
  against the local Postgres before booting — handled automatically by the
  Spring Boot Flyway starter on classpath.
- Initial `V1__init.sql` was generated from the entity shape at refactor time;
  any pre-existing demo data is incompatible. Drop and recreate the local DB
  once on upgrade.
