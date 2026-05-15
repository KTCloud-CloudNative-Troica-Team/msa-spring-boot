# 0002 — gRPC error model

## Status
Accepted — 2026-04

## Context
Domain exceptions originally extended `CustomException(code, message, status: HttpStatus, throwable)`
— a Spring HTTP type embedded in `common/exception/`. Errors never crossed
the gRPC boundary properly: a `NoSuchProductException` thrown inside the
service became `Status.UNKNOWN` at the gateway, with no machine-readable code.

## Decision
Three changes, applied together:

1. **Strip Spring from the base class.** `CustomException(code, message, status: Int, throwable)` only.
   `HttpStatusCode` is a plain `object` with `const val NOT_FOUND = 404` etc. — no `org.springframework.http` import in `domain/`.

2. **Server side — single global gRPC interceptor.** `common/grpc/GrpcExceptionMappingInterceptor`
   is annotated `@GrpcGlobalServerInterceptor` and wraps `ServerCall.close()`.
   When a `CustomException` reaches the wire (via `Status.fromThrowable`'s
   `cause` chain), the interceptor substitutes a properly mapped `Status` +
   `Metadata` trailers carrying `x-error-code` and `x-error-http-status`.
   Zero per-controller changes.

3. **Gateway side — single `@RestControllerAdvice`.** `GrpcRestExceptionAdvice`
   catches `StatusRuntimeException`, reads the trailers, and returns an
   `ExceptionBody(code, message, status)` HTTP response. Same advice also
   handles `CustomException` thrown locally in the gateway (e.g. JWT filter).

## Consequences
- Domain stays framework-free.
- Adding a new domain exception is one line; no per-service plumbing.
- Stable HTTP contract: every error response is `{code, message, status}`.
- Tradeoff: trailers carry redundant info that's also in `Status.description`.
  Acceptable — trailers are how machine-readable codes survive the round trip.
