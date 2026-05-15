# 技術監査報告書 — `msa-spring-boot`

| 項目 | 内容 |
|---|---|
| 監査対象 | `troica/msa-spring-boot` (root project `market`) |
| 監査日 | 2026-05-13 |
| 監査観点 | (1) ヘキサゴナルアーキテクチャ準拠性 (2) マイクロサービスアーキテクチャ品質 (3) Market Release 適合性 |
| 監査人 | Claude Code (Opus 4.7) |
| 総合判定 | **❌ Market Release 不適格 (Not Production-Ready)** |

---

## 0. エグゼクティブサマリー

本プロジェクトはヘキサゴナルアーキテクチャと MSA の **形** を高い水準で整えている点で技術的野心は評価できる。ポート/アダプタの分離、ドメイン層とアダプタ層のパッケージ分離、`@GrpcService` を含めた抽象クラス層の挿入、Outbox / Saga / 冪等処理 / 分散ロックの導入など、設計意図は明確である。

しかし、**実装の細部および運用観点では複数の致命的欠陥が確認された**:

- **データ永続性が存在しない**: 全サービスが `ddl-auto: create` を本番設定として持ち、再起動時に全テーブルを DROP & CREATE する。マイグレーションツール (Flyway / Liquibase) は未導入。
- **Outbox リレーが実行されない**: `processAll()` は実装されているが `@Scheduled` も REST トリガーも存在せず、Outbox レコードが永久に滞留する。これにより注文-在庫間の整合性が壊れる。
- **テストが 0 件**: 全 13 モジュールでテストファイルが存在しない (本番リリース時の最低水準を満たさない)。
- **認可ポリシーが穴だらけ**: `/api/v1/orders/**` のみ認証必須、それ以外は `anyExchange().permitAll()`。
- **ドメイン層がフレームワークに汚染**: `CustomException` が `org.springframework.http.HttpStatus`/`ResponseEntity` に依存し、すべてのドメイン例外がこれを継承。`UserDetail` (ドメイン VO) は Spring Security の `UserDetails` を実装、`OrderLineItem` (ドメイン VO) は `@Embeddable` を持つ。
- **シークレットがソース管理下**: JWT 秘密鍵、DB パスワード、Redis パスワードがすべて `application.yaml` に平文でコミットされている。
- **回復性パターン皆無**: gRPC タイムアウトなし、サーキットブレーカなし、Kafka コンシューマの DLQ / エラーハンドラなし、リトライ機構なし。
- **可観測性ゼロ**: 分散トレーシング、構造化ログ、メトリクス、相関 ID、いずれも未実装。

Market Release に到達するには、後述するロードマップに沿って **6〜8 週間の集中的な改修** が必要と見積もる。

---

## 1. 監査範囲と前提

### 1.1 対象モジュール (settings.gradle.kts)

| 種別 | モジュール |
|---|---|
| ドメインライブラリ | `common`, `inventory`, `inventory-event`, `order`, `product`, `user`, `auth` |
| アプリケーション | `inventory-service` (HTTP 8003 / gRPC 9003), `order-service` (8002/9002), `product-service` (8001/9001), `auth-service` (8005/9005), `user-api-gateway` (8100) |
| 共通インフラ | `client-redis` (Redisson ベースの分散ロック・冪等処理) |

### 1.2 アーキテクチャ概要

- **ヘキサゴナル**: `domain/` → `application/{port, service}` → `adapter/{configuration, infrastructure, presentation}` の 3 層構成。
- **MSA**: Database-per-Service (Postgres x4)、ゲートウェイ→サービスは gRPC、`order` ↔ `inventory` は Kafka 経由 (Outbox + Saga + 冪等)。
- **言語・ランタイム**: Kotlin 2.3.20, Java 23 (toolchain), Spring Boot 3.3.0.

---

## 2. ヘキサゴナルアーキテクチャの評価

### 2.1 ✅ 守られている点

| 項目 | 評価 | 根拠 |
|---|---|---|
| ドメインライブラリとアプリケーションの分離 | 良 | `inventory` (純粋ロジック) と `inventory-service` (Spring Boot エントリポイント) が分離。`common`/`client-redis` は `bootJar` 無効・`maven-publish` 有効で再利用前提。 |
| ポートとアダプタの方向性 | 良 | `application/port/outbound/` にインターフェース、`adapter/infrastructure/jpa,redis,kafka/` に実装。依存方向は正しい (アダプタ→ポート)。 |
| Command/Query 分離 | 良 | Repository を `*CommandRepository` と `*QueryRepository` に分割。Service も `*CommandService` / `*QueryService` で分離 (CQRS の素朴な適用)。 |
| Mapper の存在 | 良 | 各アグリゲートに `XxxMapper` が存在し `EntityMapper<Orm, Domain>` インターフェースを実装。ドメインエンティティと JPA エンティティが二分。 |
| Inbound ポートの DTO 自己完結 | 良 | `CreateInventoryCommand.In`/`Out` のように、ポートインターフェースが入出力データクラスを内包し、コントローラはトランスポート DTO ↔ `In`/`Out` の変換のみを担う。 |

### 2.2 ❌ 違反・問題点

#### [Critical] CV-1: ドメイン例外が Spring HTTP に依存

`common/src/main/kotlin/dev/ktcloud/black/common/exception/CustomException.kt:3-20`
```kotlin
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

open class CustomException(
    val code: String, override val message: String, val status: Int, ...
): RuntimeException(message) {
    fun toEntity(): ResponseEntity<ExceptionBody> = ...
}
```

`CustomException` は **ドメインの基底例外** として共通モジュールに置かれているが、HTTP ステータスと `ResponseEntity` をドメインに持ち込んでしまっている。これを継承する以下のすべてのドメイン例外が汚染されている:

- `inventory/.../domain/exception/InventoryException.kt:4`
- `order/.../domain/exception/OrderException.kt:4`
- `product/.../domain/exception/ProductException.kt:4`
- `user/.../domain/exception/UserException.kt:4`
- `auth/.../domain/exception/AuthException.kt:4`
- `inventory-event/.../domain/exception/InventoryEventException.kt:4`
- `order/.../outbox/.../domain/exception/OrderInventoryRequestOutboxException.kt:4`

**正解**: ドメインは純粋例外 (`InventoryException : RuntimeException`) を投げ、`@RestControllerAdvice` の `ExceptionHandler` で HTTP 変換を行う。

#### [Critical] CV-2: ドメイン VO が Spring Security に依存

`user/src/main/kotlin/dev/ktcloud/black/user/domain/vo/UserDetail.kt:3-14`
```kotlin
import org.springframework.security.core.userdetails.UserDetails
data class UserDetail(...) : UserDetails, Serializable { ... }
```

ドメイン値オブジェクトが Spring Security の `UserDetails` を実装している。これにより `user` モジュールは Spring Security 抜きでビルド不可となり、ドメインの再利用性・テスト容易性が損なわれる。

**正解**: ドメインに純粋な `UserDetail` を置き、アダプタ層で `SpringSecurityUserDetailsAdapter(user: UserDetail) : UserDetails` を用意する。

#### [Critical] CV-3: ドメイン VO に JPA アノテーション

`order/src/main/kotlin/dev/ktcloud/black/order/order/domain/vo/OrderLineItem.kt:3-13`
```kotlin
import jakarta.persistence.Embeddable
@Embeddable
data class OrderLineItem(...)
```

`domain/vo` 配下に `@Embeddable` が混入。ドメインモデルが JPA に縛られる典型的アンチパターン。

**正解**: `domain/vo/OrderLineItem` は純粋に保ち、JPA 側で `OrderLineItemJpaEmbeddable` を分離して `OrderMapper` で変換する (本プロジェクトの `Inventory` 側ですでに採用しているパターン)。

#### [Critical] CV-4: 共通基底クラスにフレームワーク依存

`common/src/main/kotlin/dev/ktcloud/black/common/domain/entity/BaseOrmEntity.kt`
```kotlin
import jakarta.persistence.{Column, EntityListeners, MappedSuperclass}
import org.springframework.data.annotation.{CreatedDate, LastModifiedDate}
import org.springframework.data.jpa.domain.support.AuditingEntityListener
```

クラス名に `Orm` が含まれているにもかかわらず、`common/domain/entity/` 配下に置かれている。命名と配置が矛盾。

**正解**: `common/adapter/infrastructure/jpa/BaseOrmEntity.kt` へ移動。`common/domain/entity/BaseDomainEntity.kt` のみがドメインに残る。

#### [Critical] CV-5: アプリケーションサービスが具象アダプタに直接依存

`user/src/main/kotlin/dev/ktcloud/black/user/application/service/UserCommandService.kt:3,13,25`
```kotlin
import dev.ktcloud.black.user.adapter.infrastructure.jpa.repository.UserPostgresqlCommandRepository

@Service
class UserCommandService(
    private val userPostgresqlCommandRepository: UserPostgresqlCommandRepository,
    ...
) : CreateUserCommand {
    override fun create(...) {
        val saved = userPostgresqlCommandRepository.save(userDomainEntity)
        ...
    }
}
```

ポート抽象 `UserCommandOutboundPort` を定義しておきながら、サービスは具象 JPA リポジトリを直接注入している。他モジュール (inventory, order, product) はすべて Outbound Port 経由を徹底しており、ここだけ抜けている。

**正解**: `UserCommandOutboundPort` を注入し、`UserPostgresqlCommandRepository` を実装側に隠す。

#### [Medium] MV-1: 永続化アダプタの命名不整合

JPA リポジトリ実装が `*Repository` 命名のままで、`*Adapter` 接尾辞を持つアダプタ命名規約と混在している:

- 一貫性なし: `InventoryPostgresqlCommandRepository`, `OrderPostgresqlQueryRepository`, ...
- 正解パターン例 (本プロジェクト内に存在): `LoadCacheSyncedInventoryPersistenceAdapter`, `AuthCacheRedisCommandAdapter`

#### [Medium] MV-2: Inbound ポートがドメインエンティティに依存

`inventory/.../application/port/inbound/command/CreateInventoryCommand.kt:3,20`
```kotlin
import dev.ktcloud.black.inventory.domain.entity.InventoryDomainEntity
data class Out(...) {
    companion object { fun from(inventory: InventoryDomainEntity): Out = ... }
}
```

`from(domainEntity)` ファクトリのためにドメインエンティティ型が漏れている。ポート層がドメインに依存することは厳密違反ではないが、変換責務は `application/service/` 側に置く方がドメイン-ポート間の循環防止に資する。

### 2.3 ヘキサゴナル準拠スコア

| 評価軸 | 評点 (5 点満点) | 備考 |
|---|---|---|
| ポート/アダプタの方向性 | 4 / 5 | CV-5 を除き正しい |
| ドメイン純粋性 | 1 / 5 | CV-1, CV-2, CV-3, CV-4 で深刻汚染 |
| Mapper による境界保護 | 4 / 5 | 実装は良好。CV-3 と矛盾 |
| Inbound/Outbound 分離 | 4 / 5 | MV-2 のみ |
| 命名規約一貫性 | 2 / 5 | MV-1 |
| **総合** | **3 / 5** | 「形は整っているが、ドメインの純粋性が崩れている」 |

---

## 3. マイクロサービスアーキテクチャの評価

### 3.1 ✅ 守られている点

| 項目 | 評価 | 根拠 |
|---|---|---|
| Database per Service | 良 | `container-compose.yaml` で Postgres を 4 インスタンス分離 (ports 7001/7002/7004/7007)。サービス間で JPA エンティティの直接参照なし。 |
| Kafka Producer 信頼性設定 | 良 | `inventory/.../KafkaConfig.kt:33-36` に `ENABLE_IDEMPOTENCE=true`, `ACKS=all`, `RETRIES=Int.MAX_VALUE`, `DELIVERY_TIMEOUT_MS=120000`。 |
| 冪等処理 (一部) | 部分 | `inventory` 側の `decrease()` は `IdempotentEventProcessor` で保護されている (`InventoryCommandService.kt:63-68`)。 |
| Outbox テーブル設計 | 部分 | `order/outbox/inventory/request/` が独立したアグリゲートとして整理されている。書き込みは同一トランザクションで実行 (`OrderCommandService.kt:19-43`)。 |
| Saga 補償 (一部) | 部分 | 在庫予約失敗時に `OrderLineItemStatus.FAILED` に遷移 (`OrderInventoryEventKafkaListener.kt:29-34`)。 |
| API バージョニング (REST) | 良 | すべてのゲートウェイ REST が `/api/v1/...` 接頭辞を持つ。 |

### 3.2 ❌ 重大欠陥

#### [Blocker] BV-1: Outbox リレーが実行されない

`order/outbox/inventory/request/application/service/OrderInventoryRequestOutboxCommandService.kt:58-69`
```kotlin
@Transactional
override fun processAll() {
    val unProcessedList = orderInventoryRequestQueryOutboundPort.fetchUnprocessed()
    runBlocking { unProcessedList.map { async(Dispatchers.IO) { processOrderInventoryRequestOutbox(it) } }.awaitAll() }
}
```

`processAll()` メソッドは存在するが、**全プロジェクトを grep しても `@Scheduled`、`SchedulingTaskExecutor`、REST エンドポイント、定期実行スレッドからの呼び出しは存在しない**。

結果として:
- 注文を作成すると Outbox レコードが保存される
- しかし Kafka には永久に発行されない
- 在庫サービスは予約要求を受け取れない
- 注文は `PENDING` のまま放置される

**この一点だけで注文フロー全体が機能しない。Market Release 以前にローカル動作も成立しない**。

**修正案**: `@Scheduled(fixedDelay = 5000)` を `processAll()` に付与し、`@EnableScheduling` をアプリケーションクラスに追加。

#### [Critical] BV-2: gRPC クライアントのタイムアウト/サーキットブレーカ未設定

`user-api-gateway/src/main/resources/application.yaml:5-18`
```yaml
grpc:
  client:
    product-service:
      address: 'static://localhost:9001'
      negotiation-type: plaintext
    ...
```

- `deadline` / `timeout` 設定なし → 下流障害時にリクエストがハングする
- Resilience4j / Spring Retry の依存性なし (`build.gradle.kts` 全体で grep 確認)
- サーキットブレーカ未導入

**修正案**: `grpc.client.*.deadline: 3s` の追加、Resilience4j-CircuitBreaker を gRPC クライアントアダプタに巻く。

#### [Critical] BV-3: Kafka コンシューマのエラーハンドラ / DLQ 未設定

`inventory/.../KafkaConfig.kt:58-73` および `order/.../KafkaConfig.kt`
- `ConcurrentKafkaListenerContainerFactory` に `setCommonErrorHandler()` の呼び出しなし
- DLT (Dead Letter Topic) 設定なし
- 例外発生時、無限リトライまたはサイレントドロップ

**修正案**: `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` の導入。

#### [Critical] BV-4: 注文側コンシューマが冪等でない

`order/.../OrderInventoryEventKafkaListener.kt:21-34`
```kotlin
override fun onResultPublished(event: InventoryReservedResultEvent) {
    if (event.resultState == InventoryReserveResultState.SUCCESS)
        orderCommandService.updateOrderLineItemStatus(...)
    if (event.resultState == InventoryReserveResultState.FAILED)
        orderCommandService.updateOrderLineItemStatus(...)
}
```

Kafka は at-least-once 配信である。在庫側は `IdempotentEventProcessor` で守られているが、**注文側は無防備**。再配信で `OrderLineItemStatus` が複数回更新される可能性がある (現状の状態遷移ロジックでは冪等になりうるが、明示的保護がない)。

**修正案**: `client-redis` の `@IdempotentEvent` を `onResultPublished` にも適用。

#### [High] BV-5: 在庫減算失敗時の Saga 補償が片手落ち

`inventory/.../InventoryCommandService.kt:60-83`

失敗時に `InventoryReserveResultEvent(state=FAILED)` を発行 → 注文側で `OrderLineItemStatus.FAILED` に変える、までは実装されている。しかし:

- **複数行の注文の一部だけ失敗** したケースで、すでに `SUCCESS` 化した他の `OrderLineItem` を **戻す (compensating reservation increase)** ロジックが存在しない
- すなわち「3 個中 1 個失敗」の場合、2 個ぶんは予約済みのまま放置される
- これは結果整合性ではなく、**整合性破壊**

**修正案**: 注文の `OrderLineItem` のいずれかが `FAILED` になった時点で、`SUCCESS` 状態のものに対し `IncreaseInventory` を補償発行する Saga コーディネータの追加。

#### [High] BV-6: ゲートウェイで認可されているのは `/orders/**` のみ

`user-api-gateway/.../SecurityConfig.kt:24-29`
```kotlin
.authorizeExchange { exchange ->
    exchange
        .pathMatchers("/swagger-ui.html", ...).permitAll()
        .pathMatchers("/api/v1/orders/**").authenticated()
        .anyExchange().permitAll()    // ← すべて素通し
}
```

- `/api/v1/products/**`, `/api/v1/inventories/**`, `/api/v1/auth/**` (auth は要素通しだが、他は要保護のはず) がすべて **無認証** で公開されている
- これは設定ミスか、設計の段階で「在庫・商品は公開で良い」と判断したかのいずれか — どちらにせよ明示的なポリシードキュメントが必要

#### [High] BV-7: サービス間 gRPC が TLS なし

`user-api-gateway/src/main/resources/application.yaml:5-18`

`negotiation-type: plaintext` がすべての gRPC クライアント設定で指定されている。クラスタ内通信であっても Zero-Trust 原則違反。Kubernetes 上で Service Mesh (Istio / Linkerd) で mTLS を被せる前提なら問題なしだが、その記述はどこにもない。

#### [High] BV-8: 可観測性 (Observability) ゼロ

全プロジェクトで以下のいずれも未導入:

- 分散トレーシング (Sleuth, Micrometer Tracing, OpenTelemetry)
- 構造化ログ (Logback JSON encoder, Logstash, ECS)
- カスタムメトリクス (Micrometer の `@Timed`, `Counter` 等)
- 相関 ID 伝播 (MDC + gRPC interceptor + Kafka header)
- カスタム `HealthIndicator` (Kafka, Redis, 下流 gRPC への到達性)

Spring Boot Actuator のデフォルト `/healthz` のみ。これでは下流 Kafka / Redis 停止時に liveness probe が green を返してしまう。

#### [High] BV-9: gRPC サービスにバージョニングなし

`product-service/src/main/proto/product.proto`
```proto
service ProductService { ... }    // ← v1 接尾辞なし
```

REST 側は `/api/v1/...` だが gRPC 側は無バージョン。サービス契約の後方互換維持戦略が存在しない。

**修正案**: `package product.v1;` または `service ProductServiceV1`。

#### [Medium] BV-10: gRPC コントローラに `@Transactional` が二重に適用される構造

サービスは `@Transactional`、Kafka リスナーから直接呼び出される箇所もあり、トランザクション境界が不明瞭。`InventoryOrderEventKafkaListener.onReserveRequest` は **メッセージ受信トランザクションと DB トランザクションが混在** している。Kafka のトランザクショナルメッセージング (transactional producer + consumer) の設計検討形跡なし。

---

## 4. プロダクションレディネス評価 (Market Release 判定)

### 4.1 [Blocker] PR-1: テスト 0 件

| モジュール | テストファイル数 |
|---|---|
| auth | 0 |
| auth-service | 0 |
| client-redis | 0 |
| common | 0 |
| inventory | 0 (ディレクトリは存在) |
| inventory-event | 0 |
| inventory-service | テストディレクトリすら未作成 |
| order | 0 |
| order-service | 同上 |
| product / product-service | 同上 |
| user / user-api-gateway | 同上 |

ユニットテスト、結合テスト、契約テスト (Pact 等)、すべて未実装。**この状態で本番投入することは不可能**。

### 4.2 [Blocker] PR-2: スキーマ管理が壊滅的

全 4 サービスで `application.yaml` に下記:
```yaml
jpa:
  hibernate:
    ddl-auto: create    # 起動のたびに全テーブル DROP & CREATE
  show-sql: true        # 全 SQL を標準出力に流す
```

- `ddl-auto: create` は **起動時に既存データを破棄** する。Market Release 環境で再起動すれば全顧客データ消失。
- Flyway / Liquibase の依存性なし (`build.gradle.kts` で確認)。`db/migration/`、`db/changelog/` ディレクトリも存在しない。
- `show-sql: true` は機密情報 (パスワードハッシュ前、PII 等) をログに流す危険性。

### 4.3 [Blocker] PR-3: シークレットがリポジトリにコミットされている

| ファイル | 漏洩内容 |
|---|---|
| `auth-service/src/main/resources/application.yaml:24` | `jwt.secret: 'v7S6A9yB2E5H8KcNfUjXnZr4u7x!A%D*G-'` |
| `auth-service/.../application.yaml:7-8` | DB user/password (`auth-service`) |
| `auth-service/.../application.yaml:20-21` | Redis password (`auth-service`) |
| `inventory-service/.../application.yaml:7-8, 20-21` | DB / Redis 認証情報 |
| `order-service/.../application.yaml:7-8` | DB 認証情報 |
| `product-service/.../application.yaml:6-8` | DB 認証情報 |
| `container-compose.yaml:1-76` | すべての DB / Redis パスワード平文 |

`${ENV:default}` パターンの環境変数置換は **どこにも使われていない**。Git history を辿れば、過去のすべてのシークレットが暴露される。

### 4.4 [Critical] PR-4: JWT アクセストークン有効期限のバグ

`auth/.../jwt/JwtGenerator.kt:16-17`
```kotlin
const val ACCESS_TOKEN_DURABILITY = 1000 * 60 * 30 * 24 * 7  // → 302,400,000ms = 84時間 = 3.5日
const val REFRESH_TOKEN_DURABILITY = 1000 * 60 * 60 * 24 * 7  // → 604,800,000ms = 7日
```

リフレッシュトークンの式 (`1000 * 60 * 60 * 24 * 7` = 7 日) と並べると、アクセストークンは「`60` の代わりに `30` を入れた」と思しき式になっており、**3.5 日の有効期間** を持つ。

しかし問題はそれ以上に: **3.5 日も 7 日も、アクセストークンの寿命としては極端に長すぎる**。業界標準は 5〜60 分。漏洩時の被害が深刻化する。

### 4.5 [Critical] PR-5: グローバル例外ハンドラ未実装

`@RestControllerAdvice` / `@ControllerAdvice` がプロジェクト内に **1 箇所も存在しない** (grep 検証済み)。

`CustomException.toEntity()` は実装されているが、呼び出すコントローラ層が存在しない → ドメイン例外が投げられても `500 Internal Server Error` + デフォルトの Spring エラーレスポンスが返るだけ。

### 4.6 [Critical] PR-6: コンテナ / CI/CD が皆無

- `Dockerfile` ファイル: 0 個
- `.github/workflows/`: 存在せず
- `.gitlab-ci.yml`, `Jenkinsfile`: 存在せず
- Kubernetes / Helm / Kustomize ディレクトリ: なし
- `deploy-submodules.bash` のみ存在するが、これは **monorepo を複数の GitHub repo に分割 + `git push --force` する破壊的スクリプト**であり、本番デプロイ手段ではない

### 4.7 [Critical] PR-7: レート制限なし

`SecurityConfig.kt` 含めゲートウェイにレート制限実装なし (Spring Cloud Gateway の `RequestRateLimiter` も未使用)。`/api/v1/auth/sign-in` 等のエンドポイントは brute-force 攻撃に対し無防備。

### 4.8 [Medium] PR-8: 不適切な HTTP ステータス

`user-api-gateway/.../UserOrderApiGatewayRestControllerAdapter.kt` の `GET` エンドポイントが `@ResponseStatus(HttpStatus.CREATED)` を返しているケースあり (Read で別途確認推奨)。

### 4.9 [Medium] PR-9: 非 LTS JVM

`build.gradle.kts:13`: `jvmToolchain(23)`。Java 23 は非 LTS。Spring Boot 3.3 のサポート行列との整合性も曖昧 (3.3 系の Java サポート確認が必要)。

### 4.10 [Low] PR-10: ドキュメント不足

`README.md` は起動手順のみ、`HELP.md` は Spring Boot 自動生成、`CLAUDE.md` のみがアーキテクチャ概観を持つ。以下未整備:

- ADR (Architecture Decision Records)
- ランブック / オンコール手順
- インシデント対応プロセス
- セキュリティ運用手順
- DB マイグレーション手順

---

## 5. 重大欠陥一覧 (優先度マトリクス)

| ID | カテゴリ | 重大度 | 概要 | 修正工数 (人日) |
|---|---|---|---|---|
| BV-1 | 機能 | **Blocker** | Outbox リレー未起動 — 注文フローが動作しない | 0.5 |
| PR-2 | データ | **Blocker** | `ddl-auto: create` + マイグレーション無し | 5 |
| PR-1 | 品質 | **Blocker** | テスト 0 件 | 20+ |
| PR-3 | セキュリティ | **Blocker** | シークレットがリポジトリにコミット | 2 |
| PR-6 | 運用 | **Blocker** | Dockerfile/CI/CD なし | 8 |
| CV-1〜4 | アーキ | Critical | ドメイン層がフレームワークに汚染 | 4 |
| CV-5 | アーキ | Critical | アプリ層が具象リポジトリに依存 | 0.5 |
| BV-2 | 回復性 | Critical | gRPC タイムアウト/サーキットブレーカなし | 2 |
| BV-3 | 回復性 | Critical | Kafka コンシューマ DLQ なし | 2 |
| BV-4 | 整合性 | Critical | 注文側コンシューマが非冪等 | 1 |
| BV-6 | セキュリティ | Critical | 認可ルールが穴だらけ | 0.5 |
| PR-4 | セキュリティ | Critical | JWT アクセストークン 3.5 日 | 0.2 |
| PR-5 | API | Critical | グローバル例外ハンドラなし | 1 |
| PR-7 | セキュリティ | Critical | レート制限なし | 1 |
| BV-5 | 整合性 | High | Saga 部分失敗の補償なし | 3 |
| BV-7 | セキュリティ | High | gRPC 平文通信 (Service Mesh 前提が未文書化) | 1 |
| BV-8 | 可観測性 | High | トレーシング / 構造化ログ / メトリクス全欠 | 4 |
| BV-9 | API | High | gRPC バージョニングなし | 0.5 |
| BV-10 | 整合性 | Medium | Kafka × DB のトランザクション境界曖昧 | 2 |
| MV-1, MV-2 | アーキ | Medium | 命名不整合 / ポート漏れ | 1 |
| PR-8 | API | Medium | HTTP ステータス誤り | 0.2 |
| PR-9 | プラットフォーム | Medium | 非 LTS JVM | 0.5 |
| PR-10 | ドキュメント | Low | ADR / ランブック未整備 | 3 |

**合計推定工数: 約 62 人日 (6〜8 週間 / FTE 1 人)**

---

## 6. 改善ロードマップ

### Phase 0 — 即時対応 (1 週間以内、Blocker 解消)

1. **BV-1** Outbox リレーに `@Scheduled(fixedDelay = 5000)` を追加 + `@EnableScheduling` (0.5 日)
2. **PR-2** Flyway 導入、すべての `ddl-auto: create` → `validate`、`V1__init.sql` を起こす (5 日)
3. **PR-3** すべてのシークレットを `${...}` で環境変数化、`.env.example` 提供、Git history から漏洩シークレットを **必ずリボーク** (2 日)
4. **PR-4** アクセストークンを 15 分、リフレッシュを 14 日に修正 (0.2 日)
5. **PR-7** ゲートウェイに `RequestRateLimiter` (Redis ベース) を導入 (1 日)
6. **BV-6** 認可ポリシーを白リスト方式に変更 (`.anyExchange().authenticated()` をデフォルト) (0.5 日)

### Phase 1 — アーキテクチャ衛生 (2〜3 週目)

1. **CV-1**〜**CV-5** ドメイン純粋性の回復 (4.5 日)
   - `CustomException` から `HttpStatus`/`ResponseEntity` を剥がす
   - `UserDetail` から `UserDetails` 実装を分離
   - `OrderLineItem` から `@Embeddable` を剥がす
   - `BaseOrmEntity` を `adapter/infrastructure/jpa/` へ移動
   - `UserCommandService` を `UserCommandOutboundPort` 経由に変更
2. **PR-5** `@RestControllerAdvice` を gateway / 各 service 用に作成 (1 日)
3. **BV-9** すべての proto を `package <service>.v1;` に変更 (0.5 日)
4. **MV-1, MV-2** リポジトリアダプタ命名統一、Inbound ポート DTO 整理 (1 日)

### Phase 2 — 回復性と整合性 (4〜5 週目)

1. **BV-2** Resilience4j 導入、gRPC クライアントにサーキットブレーカ + タイムアウト + バルクヘッド (2 日)
2. **BV-3** Kafka `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` (2 日)
3. **BV-4** 注文側コンシューマに `@IdempotentEvent` 適用 (1 日)
4. **BV-5** Saga 部分失敗の補償 (`IncreaseInventory` 補償アクション) 実装 (3 日)
5. **BV-10** Kafka transactional messaging 設計、トランザクション境界の文書化 (2 日)

### Phase 3 — 可観測性と運用 (6〜7 週目)

1. **BV-8** Micrometer Tracing + OpenTelemetry、Logback JSON encoder、相関 ID 伝播 (gRPC interceptor + Kafka header) (4 日)
2. カスタム `HealthIndicator`: Kafka, Redis, downstream gRPC (1 日)
3. **PR-6** Dockerfile (multi-stage), GitHub Actions (test → build → image → push), Helm chart の骨組み (8 日)

### Phase 4 — 品質保証 (8 週目以降)

1. **PR-1** テスト整備 (20 日+, 並列推奨)
   - ドメイン層ユニットテスト (Kotest 推奨)
   - アプリケーション層モックテスト (MockK)
   - アダプタ層結合テスト (Testcontainers: Postgres + Kafka + Redis)
   - 契約テスト (Pact for gRPC, OpenAPI for REST)
   - E2E (Playwright + Docker Compose)
2. **PR-10** ADR (主要 10 件)、ランブック、インシデント対応プロセスの整備 (3 日)

---

## 7. 結論

### 7.1 設計の評価

`msa-spring-boot` は **教育目的・ポートフォリオとしては優れた構造を持つ**。ヘキサゴナル、MSA、Saga、Outbox、CQRS、分散ロック、冪等処理という現代的な分散システムパターンが網羅的に試みられており、命名規約と多モジュール分割も意図的である。

しかし、**それらの設計意図が「動作する状態」まで実装され切っていない**。Outbox リレーが起動しないという初歩的欠陥が、注文フロー全体を死に追いやっている事実は、品質保証 (テスト) の欠如そのものが原因である。

### 7.2 Market Release 判定

| 基準 | 判定 |
|---|---|
| 機能完結性 (注文フローが通る) | ❌ **NG** (BV-1) |
| データ永続性 | ❌ **NG** (PR-2) |
| セキュリティ (秘密管理・認可) | ❌ **NG** (PR-3, BV-6, PR-7) |
| 回復性 (障害許容) | ❌ **NG** (BV-2, BV-3, BV-5) |
| 可観測性 | ❌ **NG** (BV-8) |
| デプロイ可能性 | ❌ **NG** (PR-6) |
| 品質保証 (テスト) | ❌ **NG** (PR-1) |
| アーキテクチャ整合性 | ⚠️ **要改善** (CV-1〜5) |

**結論: 現状の `msa-spring-boot` は Market Release 水準に**到達していない**。**

本プロジェクトはむしろ **「アーキテクチャ学習教材」または「PoC 段階のスケルトン」** として位置付けるのが正確である。プロダクション運用に進むには、上記ロードマップ Phase 0〜4 をすべて完遂する必要があり、その所要工数は概算で **62 人日 (6〜8 週間 / 1FTE)** である。

### 7.3 短期推奨アクション (即日対応推奨)

1. `ACCESS_TOKEN_DURABILITY` の値を `1000 * 60 * 15` (15 分) に修正
2. `application.yaml` 内のすべてのシークレットを `${ENV:default}` 形式に変換、Git history からの漏洩値をリボーク
3. `OrderInventoryRequestOutboxCommandService.processAll()` に `@Scheduled(fixedDelay = 5000)` 追加 + `@EnableScheduling`
4. `SecurityConfig.filterChain` の `.anyExchange().permitAll()` を `.authenticated()` に変更し、公開エンドポイントのみ明示的に `.permitAll()`
5. すべての `application.yaml` の `ddl-auto: create` を `validate` または `none` に変更し、Flyway 導入計画を立てる

以上 5 点だけでも対応すれば、最低限の「ローカル起動で注文が成立する状態」と「秘密漏洩リスクの止血」が達成可能である。

---

*— End of Audit Report —*
