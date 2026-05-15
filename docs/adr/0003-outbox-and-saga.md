# 0003 — Outbox + saga compensation

## Status
Accepted — 2026-04

## Context
`order-service` → `inventory-service` stock-reservation must be atomic *with* the
order write, but Kafka publish is not a database transaction. Audit BV-1 found
that the existing `OrderInventoryRequestOutboxCommandService.processAll()` was
never scheduled, so orders sat in the outbox forever.

Audit BV-5 also found that partial-failure compensation was missing — if line
item 1 succeeded but line item 2 failed, the inventory reserved for line 1
stayed locked forever.

## Decision

### Outbox (reserve direction)
- `OrderCommandService.create(...)` writes the order **and** outbox rows in the
  same `@Transactional` boundary.
- A separate `@Scheduled(fixedDelayString = "${order.outbox.relay.fixed-delay-ms:5000}")`
  drains the outbox to Kafka. Failures bump `retry`; after a maximum, the row
  flips to `FAILED` (manual remediation).
- `@EnableScheduling` is on `OrderServiceApplication`.

### Compensation (release direction)
- A new topic `inventory-release-request-topic` carries
  `InventoryReleaseRequestEvent(orderId, inventoryId, amount)`.
- In `OrderCommandService.updateOrderLineItemStatus`, when a line transitions
  to FAILED, the service walks peers still in `INVENTORY_RESERVED` and emits a
  release event per peer. Each released peer transitions to
  `INVENTORY_RELEASED`.
- `InventoryReleaseEventKafkaListener` on the inventory side consumes and
  calls `IncreaseInventoryCommand`.
- Release publish is **best-effort, not outboxed.** Idempotency on the
  inventory consumer absorbs Kafka redelivery and order-side retries.

## Consequences
- Reserve direction: at-least-once delivery, no duplicate decrements (consumer
  uses `IdempotentEventProcessor`).
- Release direction: at-least-once with idempotent absorption, but if the
  order side crashes after marking peers `INVENTORY_RELEASED` but before
  Kafka ack, the release is *not* retried. Acceptable for now; a future
  iteration could outbox releases too.
- New terminal state `INVENTORY_RELEASED` is independent of order overall
  status (stays `FAILED`).
