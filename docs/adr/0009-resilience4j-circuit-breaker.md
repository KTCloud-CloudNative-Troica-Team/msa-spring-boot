# 0009 — Resilience4j circuit breakers

## Status
Accepted — 2026-04

## Context
Audit BV-2: gRPC client calls had no timeout and no breaker. A downstream
hang would propagate to gateway threads indefinitely.

## Decision
- `grpc.client.<name>.deadline: ${GRPC_CLIENT_DEADLINE:3s}` set on every
  `@GrpcClient` in both gateways.
- Each gateway-side service class (the `@Service` that holds the `@GrpcClient`
  stub) has `@CircuitBreaker(name = "<downstream-name>")` on every
  `override suspend fun` that calls the stub.
- Defaults per breaker:
  - sliding window: 20 calls
  - failure threshold: 50%
  - open-state wait: 10 s
  - half-open trial: 5 calls
  - registers a health indicator so `/healthz` reflects breaker state.

## Consequences
- A downstream's first slow streak triggers fail-fast within ~10 calls,
  shielding upstream.
- 3-second deadline is aggressive — verify against p99 of legitimate slow
  calls (e.g. order list with many items) before tightening.
- Breaker state is **per gateway instance**. Multiple gateway replicas open
  their breakers independently. Acceptable for now.
- Fallback methods are not yet wired — open breakers throw
  `CallNotPermittedException`, which `GrpcRestExceptionAdvice` returns as
  503 via `Status.UNAVAILABLE` mapping.
