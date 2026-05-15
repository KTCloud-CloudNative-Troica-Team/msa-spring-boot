# 0001 — Hexagonal module split

## Status
Accepted — 2026-04

## Context
Each business domain (product, order, inventory, auth, user) is exposed over both
gRPC (server-to-server) and HTTP (gateway-fronted). Tests, build artifacts, and
deployment units don't align 1:1 with domain code — we want domain logic that
can be unit-tested with no Spring context, but `*-service` modules that wire a
single `main()` for production.

## Decision
Two kinds of Gradle modules per domain:

- **Domain library** (`order`, `inventory`, …): pure Kotlin + Spring stereotypes.
  No `main()`, no proto, no Boot autoconfig. `domain/` is framework-free
  (no Spring, no JPA, no Jakarta). `application/port/` is the contract surface;
  `adapter/infrastructure/{jpa,redis,kafka}` provides the implementations.
- **Service module** (`order-service`, `user-api-gateway`, …): tiny Boot
  application that depends on one or more domain libraries and exposes a
  network port. Owns `.proto` files, `application.yaml`, and `Application.kt`.

Domain modules cannot depend on service modules. Service modules cannot
depend on each other.

## Consequences
- Domain ⇒ service is the only allowed direction; circular deps are impossible.
- `common` is published to Maven so domain libs can be reused externally
  (`com.github.kanei0415:ktcloud-market-msa-common`).
- Adding a new bounded context = adding two modules. Acceptable cost.
- Domain tests can run without Spring (MockK + JUnit), which the audit
  identified as the most-neglected layer but is now structurally feasible.
