# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

Toolchain: Java 23, Kotlin 2.3.20, Spring Boot 3.3.0, Gradle multi-module (root `market`).

```bash
./gradlew generateProto        # Required before first build/run — generates gRPC + Kotlin proto stubs
./gradlew kaptKotlin            # Required when JPA entities change — generates QueryDSL Q-classes
docker-compose -f container-compose.yaml up -d   # Start Postgres / Redis / Kafka local middleware
./gradlew :<service>:bootRun    # Run an individual Spring Boot service
./gradlew :<module>:test        # Run tests for a single module
./gradlew :<module>:test --tests "ClassName.methodName"   # Run a single test
```

Bootable services (each is `<name>-service` and exposes both HTTP and gRPC):

| Service | HTTP | gRPC | Backing stores |
|---|---|---|---|
| `user-api-gateway` | 8100 | – | gRPC client of the four services below; Swagger at `/webjars/swagger-ui/index.html` |
| `product-service` | 8001 | 9001 | Postgres 7001 |
| `order-service` | 8002 | 9002 | Postgres 7002, Kafka 7003 |
| `inventory-service` | 8003 | 9003 | Postgres 7004, Redis 7005, Kafka 7003 |
| `auth-service` | 8005 | 9005 | Postgres 7007, Redis 7006 |

JPA `ddl-auto: validate` (overridable via `JPA_DDL_AUTO`); schema is owned by **Flyway** (`src/main/resources/db/migration/V*__*.sql`). Adding a column means writing a new `V<n>__*.sql` and updating the JPA entity. There is no `create` fallback in normal operation.

Required environment variables are documented in `.env.example` — every secret (`JWT_SECRET`, `*_DB_PASSWORD`, `*_REDIS_PASSWORD`) reads from env with a local default. **Do not commit production values to this repo**; rotate any secret that ever appeared in git history.

## Architecture

### Module layout (settings.gradle.kts)

Two kinds of Gradle modules, deliberately split:

- **Domain library modules** — `inventory`, `inventory-event`, `order`, `product`, `user`, `auth`, `common`: pure business logic, no `main()`. Compiled as plain JARs (`bootJar` disabled on `common` and `client-redis`).
- **Service modules** — `<name>-service` and `user-api-gateway`: thin Spring Boot apps that wire one or more domain libs and expose gRPC/HTTP. They contain `XxxApplication.kt` with `@SpringBootApplication(scanBasePackages = ["dev.ktcloud.black"])` so beans from any domain lib on the classpath are picked up.

`common` and `client-redis` are also published as Maven artifacts (`com.github.kanei0415:ktcloud-market-msa-common`, `…-client-redis`). `inventory` already consumes `client-redis` from jitpack (`com.github.kanei0415:ktcloud-msa-client-redis:v1.0.2`) rather than the local `:client-redis` project — keep this in mind if you change `client-redis`: local changes don't reach `inventory` until a new jitpack tag is published.

`deploy-submodules.bash` splits this monorepo into per-service GitHub repos (mapping at the top of the script). It runs `git init` + `git push --force` on each output dir, so edits to that script are destructive — review carefully.

`template/` is scaffolding for new modules and is intentionally NOT in `settings.gradle.kts`.

### Hexagonal layout inside each domain module

Every domain module follows the same ports-and-adapters package shape, e.g. `dev.ktcloud.black.inventory`:

```
domain/             entity/, vo/, event/, exception/   — pure Kotlin, no Spring
application/
    port/
        inbound/    XxxCommand / XxxQuery interfaces with nested In / Out data classes
        outbound/   XxxCommandOutboundPort / XxxQueryOutboundPort
        event/      XxxEventPublishPort / XxxEventListenerPort
        cache/, state/   further outbound port groupings when needed
    service/        XxxCommandService / XxxQueryService — implement multiple inbound ports
    dto/
adapter/
    configuration/  Spring @Configuration (kafka, redis, security, jpa…)
    infrastructure/ jpa/, redis/, kafka/ — implement outbound ports
    presentation/   web/inbound REST or gRPC controllers
```

Conventions to preserve when adding code:

- **Inbound ports define their own `In` / `Out` data classes** (see `application/port/inbound/command/DecreaseInventoryCommand.kt`). Controllers translate transport DTOs ↔ `In`/`Out`; services never see proto/REST types. **The port must not reference any `XxxDomainEntity`** — conversion from a domain entity to `Out` is the service's job (constructed inline). This keeps `application/port/` symmetric: it depends on `domain/vo` and primitive types only.
- **Two entity types per aggregate**: `XxxDomainEntity` (in `domain/entity`, framework-free) and a JPA `Xxx` entity (in `adapter/infrastructure/jpa/entity`). An `XxxMapper` translates between them. `BaseDomainEntity` lives in `common/domain/entity/`; `BaseOrmEntity` lives in `common/adapter/infrastructure/jpa/` (JPA annotations stay out of `domain/`).
- **Domain exceptions extend `CustomException`** (in `common/exception/`) which carries `code: String`, `message: String`, `status: Int` only. The `status` int is one of the constants in `HttpStatusCode` — there is no `org.springframework.http.HttpStatus` import allowed in domain. HTTP / gRPC translation lives in `GrpcRestExceptionAdvice` (gateway) and `GrpcExceptionMappingInterceptor` (common, server-side).
- **JPA repository naming**: each aggregate ships `XxxPostgresqlCommandRepository` and `XxxPostgresqlQueryRepository` (both `@Component`-annotated `OutboundPort` implementations), and they delegate to a Spring Data `XxxPostgresqlRepository` interface. The `*PersistenceAdapter` suffix in a couple of legacy classes (`LoadCacheSyncedInventoryPersistenceAdapter`, `AuthCacheRedisCommandAdapter`) is not the canonical name — new code uses `*PostgresqlCommandRepository` / `*PostgresqlQueryRepository`.
- **Service classes implement multiple inbound port interfaces** rather than exposing public methods directly (e.g. `InventoryCommandService : CreateInventoryCommand, DecreaseInventoryCommand, IncreaseInventoryCommand`). Callers depend on the narrow port, not the service.

### Cross-service communication

Two transports, used for different reasons:

1. **gRPC (synchronous)** — `user-api-gateway` → each `<name>-service`. Each service module owns its `.proto` under `src/main/proto/`; the gateway re-declares the same protos so its generated stubs match. Server-side: `@GrpcService XxxGrpcControllerAdapter` extends an abstract `XxxGrpcController` that itself extends `XxxServiceGrpcKt.XxxServiceCoroutineImplBase` — the abstract layer exists so controller logic can be unit-tested without the gRPC base.
2. **Kafka (asynchronous)** — `order-service` ↔ `inventory-service` only. Topics:
   - `inventory-reserve-request-topic` (order → inventory): decrement requests, fed by the outbox.
   - `inventory-reserved-result-topic` (inventory → order): SUCCESS/FAILED per line item.
   - `inventory-release-request-topic` (order → inventory): **saga compensation** — when one line item in a multi-line order FAILS, the order side publishes one release event per already-RESERVED peer line item, and inventory re-increments stock. The order's per-line status moves `INVENTORY_RESERVED → INVENTORY_RELEASED`.
   - Each topic has a sibling `*-dlt` topic; `DefaultErrorHandler + DeadLetterPublishingRecoverer` routes poison messages there after 3 retries with a 1 s back-off.
   `order/outbox/inventory/request/...` implements the **outbox pattern** for the reserve path only — orders are persisted with an outbox row, and `OrderInventoryRequestOutboxCommandService.processAll()` (annotated `@Scheduled(fixedDelayString = "\${order.outbox.relay.fixed-delay-ms:5000}")`) drains the outbox to Kafka. The compensation (release) publish is **best-effort, not outboxed** — duplicates are absorbed by `IdempotentEventProcessor` on the inventory consumer side. The `inventory-event` module is a sibling library that records consumed events for idempotency on the inventory side.

### Transactional boundaries (Kafka × DB)

- **Producer side** uses `enable.idempotence=true`, `acks=all`, `retries=MAX_VALUE`, `delivery.timeout.ms=120000`. No Kafka transactional producer is configured — the outbox guarantees at-least-once delivery to Kafka, and consumers are idempotent.
- **Consumer side** does NOT participate in a Kafka transaction. Each listener method runs in its own DB transaction (`@Transactional` on the called service method). Kafka offset commit happens after the listener returns successfully; failure routes through `DefaultErrorHandler` → DLT, so partial DB writes on failure are still rolled back independently of the offset commit.
- Idempotency: every inbound listener is wrapped in `IdempotentEventProcessor.withIdempotencyProcess(key, ttl)` (Redis-backed via `client-redis`). Keys are stable per `(orderId, inventoryId, eventType)` so Kafka redelivery is a no-op. **`order-service` shares `inventory-service-redis` (port 7005)** in local compose for this purpose — split into a dedicated Redis before any non-local deploy.

### Concurrency primitives (`client-redis`)

Two reusable APIs that domain services depend on rather than touching Redis directly:

- `DistributedLock.execute(key, func, …)` — Redisson `tryLock` wrapper. Used in `InventoryCommandService` around cache-rehydration (`InventoryLockKey`).
- `IdempotentEventProcessor.withIdempotencyProcess(key, ttl, func)` — sets a `idempotent-processed<key>` marker after `func` runs successfully; returns `null` on replay. Used to make `decrease()` safe under at-least-once Kafka delivery (`InventoryDecreaseIdempotencyKey`).

When adding new cache/lock keys, define a VO under `domain/vo` with a `toLockKey()` / `toIdempotencyKey()` method rather than building strings inline.

### Auth flow

`auth-service` issues JWT (HS256, `jwt.secret` from yaml) and stores refresh tokens in its own Redis. `JwtResolver` decodes claims to a `UserDetail(id, role, email, name)` VO. The gateway carries security config (Spring Security on WebFlux) and validates tokens before forwarding to downstream gRPC services.

## Kotlin / Spring details worth knowing

- Root `build.gradle.kts` applies `kotlin-spring`, `kotlin-jpa`, `spring-boot`, and `dependency-management` to all subprojects — module `build.gradle.kts` files only declare extra plugins (e.g. `kapt`, `protobuf`) and dependencies.
- Kotlin compiler args include `-Xannotation-default-target=param-property`, which affects where annotations on `val` constructor params land. Be deliberate with `@field:` / `@get:` only when overriding this default.
- Protobuf-generated sources are added back into `main` via `sourceSets { srcDirs("build/generated/source/proto/main/{java,kotlin}") }` in each `*-service` and the gateway. Same trick for kapt'd QueryDSL Q-classes in `order`, `common`, `inventory-event`.

## Things that aren't in the code

- gRPC server runs `plaintext` between services. **mTLS is expected to be terminated at the Service Mesh layer** (Istio / Linkerd) — there is no in-process TLS config. Local dev runs without a mesh.
- There is no top-level test suite; tests live per module under `src/test/kotlin` and are sparse. `inventory/src/test` has the most scaffolding.
