# 0004 — Redis-backed idempotency

## Status
Accepted — 2026-04

## Context
Kafka delivery is at-least-once. The audit (BV-4) found the order-side consumer
processed events naively, so redelivery would re-flip `OrderLineItemStatus`
spuriously. The inventory side already had `IdempotentEventProcessor` for
`decrease()`.

## Decision
- Every Kafka listener wraps its handler in `IdempotentEventProcessor.withIdempotencyProcess(key, ttl)`.
  The key is built from a domain VO (e.g. `OrderInventoryResultIdempotencyKey(orderId, inventoryId, resultState)`).
- VOs encapsulate the key string format. Listeners pass `key.toIdempotencyKey()`.
- TTL defaults to 30 min (the Kafka retention horizon we assume).
- Implementation: Redisson `RBucket` + `DistributedLock` (in `client-redis`).
- `order-service` shares `inventory-service-redis` (port 7005) in local compose.
  Keys are prefix-namespaced (`order-inventory-result:`, `inventory-release:`).
  **Before non-local deploy, give each service its own Redis instance.**

## Consequences
- Re-deliveries are no-ops.
- Failures inside the handler do NOT mark the key as DONE, so retry replays correctly.
- Tradeoff: redis is now a hard dependency for both services. Health indicators
  must surface it (Spring Boot auto-configures `RedisHealthIndicator`).
- Sharing redis breaks database-per-service. Documented as a known compromise
  awaiting infrastructure work.
