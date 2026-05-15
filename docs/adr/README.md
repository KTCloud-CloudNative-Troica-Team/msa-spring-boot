# Architecture Decision Records

Numbered, append-only records of the load-bearing decisions in `msa-spring-boot`.
New decisions get a new ADR; supersede an older one only by writing a follow-up
ADR that links back and explicitly marks the prior one `Superseded`.

| # | Title | Status |
|---|---|---|
| [0001](0001-hexagonal-module-split.md) | Hexagonal module split | Accepted |
| [0002](0002-grpc-error-model.md) | gRPC error model | Accepted |
| [0003](0003-outbox-and-saga.md) | Outbox + saga compensation | Accepted |
| [0004](0004-idempotency-strategy.md) | Redis-backed idempotency | Accepted |
| [0005](0005-jwt-token-lifetime.md) | JWT token lifetime | Accepted |
| [0006](0006-secret-management.md) | Secret management strategy | Accepted |
| [0007](0007-flyway-schema-management.md) | Flyway-managed schema | Accepted |
| [0008](0008-grpc-mtls-via-service-mesh.md) | gRPC plaintext + mesh mTLS | Accepted |
| [0009](0009-resilience4j-circuit-breaker.md) | Resilience4j circuit breakers | Accepted |
| [0010](0010-proto-package-versioning.md) | Proto package versioning | Accepted |
