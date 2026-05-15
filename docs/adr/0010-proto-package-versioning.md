# 0010 — Proto package versioning

## Status
Accepted — 2026-04

## Context
Audit BV-9: REST routes were versioned (`/api/v1/...`) but proto services
were not. The wire identifier for every service was bare `<service>.service`,
giving no path for backward-incompatible iteration.

## Decision
- Every `.proto` declares `package <service>.v1;` (e.g. `product.v1`,
  `order.v1`). The wire-format fully-qualified service name becomes
  `product.v1.ProductService`.
- `option java_package = "...";` is **unchanged**, so Kotlin import paths
  stay stable across the v1 → v2 cutover (we'd only add new classes in a
  parallel package).
- Both gateway proto copies and `*-service` proto sources are bumped
  together; clients and servers move atomically since we control both ends.

## Consequences
- Breaking changes to a service contract require a new `<service>.v2` package
  + a new `ServiceServiceV2` running alongside V1 until clients migrate.
- No Kotlin import churn for the v1 introduction (`java_package` unchanged).
- Tradeoff: the proto file path doesn't reflect the version — could rename to
  `product/v1/product.proto` later if multi-version proto sources coexist.
