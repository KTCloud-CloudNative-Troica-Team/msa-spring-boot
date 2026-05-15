# SERVICE_PLAN — E-Commerce プラットフォーム全体構想

> 作成日: 2026-05-15
> 対象リポジトリ: `msa-spring-boot` (Kotlin 2.3 / Spring Boot 3.3 / Java 23 / gRPC / Kafka)
> 想定読者: バックエンド・インフラ・SRE・プロダクトオーナー

---

## 0. エグゼクティブサマリー

本リポジトリは現時点で **`product` / `order` / `inventory` / `auth` / `user-api-gateway`** の 5 サービスから成る MSA の骨格を持っており、ヘキサゴナルアーキテクチャ・gRPC 同期通信・Kafka + Outbox による非同期通信・Redis 分散ロック・冪等性処理など、本格的な EC プラットフォームに必要な「土台」はすでに整備されている。

しかし「EC サービスを事業として成立させる」には、以下が決定的に不足している。

1. **決済 (Payment) / 配送 (Shipping) / 通知 (Notification) / 検索 (Search) / レビュー (Review) / クーポン (Promotion) / 推薦 (Recommendation)** といったドメインサービスが未実装。
2. **可観測性 (Observability)・セキュリティ (Secrets, mTLS)・CI/CD・本番デプロイ基盤** が皆無 (`ddl-auto: create`、yaml 直書き JWT 秘密鍵、Docker Compose のみ)。
3. **データ整合性戦略**(Saga、CDC、Outbox の網羅的適用)が `order ↔ inventory` の 1 ペアにしか適用されていない。

本ドキュメントは、これらを **Phase 0 〜 Phase 4** の段階的ロードマップに沿って解消し、最終的に「数百万 SKU・ピーク時 10k RPS・99.95% SLO」を満たす EC バックエンドへ到達するための計画である。

---

## 1. プロダクトビジョンと非機能要件 (NFR)

### 1.1 ビジョン
「在庫の正確性」と「決済の信頼性」を最優先しつつ、**AI による検索・推薦体験**で差別化する B2C 型 EC プラットフォームを構築する。

### 1.2 非機能要件 (目標値)

| 区分 | 指標 | 目標 |
|---|---|---|
| 可用性 | 月間ダウンタイム | 99.95% (≒ 21 分/月) |
| レイテンシ | 商品一覧 API p99 | < 200ms |
| レイテンシ | 注文確定 API p99 | < 800ms |
| スループット | ピーク注文 | 1,000 orders/sec |
| データ整合性 | 在庫オーバーセル率 | 0.001% 未満 |
| セキュリティ | OWASP ASVS | Level 2 |
| 復旧性 | RPO / RTO | RPO 5min / RTO 30min |

---

## 2. 現状アーキテクチャの評価

### 2.1 強み (継続活用)

- **ヘキサゴナル + ポートアダプタ**:`application/port/inbound|outbound` が明確に分離され、テスト容易性が高い。
- **`In/Out` データクラスをポートが定義**:プロトコル変更が service 層へ波及しない。
- **`DomainEntity` と `OrmEntity` の分離 + Mapper**:DB 都合がドメインに漏れない。
- **`DistributedLock` + `IdempotentEventProcessor` の VO ベース API**:鍵生成のロジックが集約されており、新規ユースケースで再利用可能。
- **Outbox パターン**:`order/outbox/inventory/request` がすでに at-least-once 配信を担保。

### 2.2 リスクと負債

| 項目 | 現状 | 影響 |
|---|---|---|
| `ddl-auto: create` | 起動毎に DROP/CREATE | 本番投入不可、マイグレーション戦略が必要 |
| `jwt.secret` の yaml 直書き | コミット済み | 即座にローテート + Secrets Manager へ |
| 単一 Postgres インスタンス × サービス | 7001/7002/7004/7007 | 本番では HA / Read Replica 構成が必要 |
| `client-redis` を jitpack 経由で `inventory` が消費 | ローカル変更が反映されない | モノレポ内では `:client-redis` を直接参照すべき (Phase 0 で是正) |
| 観測手段ゼロ | ログのみ | Tracing / Metrics / Log を OpenTelemetry で統一 |
| テスト網羅率が低い | `inventory` のみ scaffolding | Phase 1 で契約テスト + 受入テストを追加 |
| `deploy-submodules.bash` が `--force push` | レビュー機構なし | CI 化 + Dry-run モード必須 |

---

## 3. 目標アーキテクチャ

### 3.1 全体図 (テキスト ASCII)

```
                     ┌─────────────────────────────┐
   Client (Web/App)─►│   CDN / WAF / Edge (Cloudflare)
                     └────────────┬────────────────┘
                                  ▼
                ┌────────────────────────────────────┐
                │   user-api-gateway (BFF, WebFlux)  │  ← JWT 検証・Rate Limit・Tracing 起点
                └──┬─────┬─────┬─────┬─────┬─────┬───┘
        gRPC      │     │     │     │     │     │
   ┌──────────────┼─────┼─────┼─────┼─────┼─────┼──────────────┐
   ▼              ▼     ▼     ▼     ▼     ▼     ▼              ▼
 product       order  inv.  auth  pay.  ship  promo …      search-svc
 -service                                                  (OpenSearch
                                                          +Vector Idx)
   │              │     │           │     │
   ▼ Postgres    ▼ PG  ▼ PG/Redis  ▼ PG  ▼ PG
                  │     │           │
                  └─────┴── Kafka ──┴── Debezium CDC ──► Data Lake (Iceberg)
                                                              │
                                                              ▼
                                                       ML Feature Store
                                                              │
                                                              ▼
                                                  recommendation-service
```

### 3.2 通信戦略

| パターン | 用途 | 例 |
|---|---|---|
| **gRPC (sync)** | ユーザー対話の読み取り・即時整合が必要な書き込み | gateway → product / order |
| **Kafka (async, Outbox)** | サービス間の状態伝播・イベント駆動 | order → inventory, order → notification |
| **Debezium CDC** | DB → 検索・分析・ML 特徴量 | product PG → OpenSearch / Iceberg |
| **WebSocket / SSE** | 配送追跡・在庫枯渇のリアルタイム通知 | gateway → client |

### 3.3 データ整合性方針

- **強整合**:単一サービス内 (Postgres トランザクション)。
- **結果整合**:サービス境界をまたぐ全ての書き込み伝播 → **Outbox + Idempotency Key** を必須化。
- **Saga (オーケストレーション型)**:注文確定フロー (`order → inventory → payment → shipping`) は専用の `order-saga-coordinator` を新設して状態機械で管理。
- **補償トランザクション**:各ステップに `cancel`/`refund`/`restock` を必ず実装。

---

## 4. モジュール計画

### 4.1 既存モジュールの再整理 (Phase 0)

| モジュール | アクション |
|---|---|
| `common`, `client-redis` | jitpack 依存をやめて `inventory` も `:client-redis` を直接参照。バージョニングは Renovate で半自動化。 |
| `auth-service` | `jwt.secret` を環境変数 + Secrets Manager 経由に。リフレッシュトークン回転を導入。 |
| 全 `*-service` | Flyway を導入し、`ddl-auto: validate` に切替。 |
| `user-api-gateway` | Resilience4j (Circuit Breaker / Retry / Bulkhead) を導入。 |

### 4.2 新規追加モジュール (Phase 1〜3)

優先順位順に列挙。各モジュールはこれまでの命名規約 (`<domain>` ライブラリ + `<domain>-service` ブート) に従う。

#### 【Phase 1 — MVP 完成】

1. **`payment` / `payment-service`** (最優先)
   - 決済 PG 連携 (Stripe / PayPay / KG イニシス等の差し替え可能なアダプタ層)。
   - 二段階キャプチャ (Authorize → Capture) と PCI-DSS スコープ縮小のため **トークン化** に限定。
   - 冪等鍵 (`Idempotency-Key` ヘッダ) を必須化。
   - イベント: `PaymentAuthorized`, `PaymentCaptured`, `PaymentFailed`, `PaymentRefunded`。

2. **`shipping` / `shipping-service`**
   - 配送業者アダプタ (ヤマト / 佐川 / CJ大韓通運 / 日本郵便)。
   - 追跡番号管理、配送状態の Webhook 受信、SSE で gateway へ転送。

3. **`notification` / `notification-service`**
   - チャネル抽象 (`Email` / `SMS` / `LINE` / `Push (FCM/APNs)` / `In-App`)。
   - **テンプレートエンジン + i18n** (日本語・韓国語・英語)。
   - **配信抑制ポリシー** (rate-limit, do-not-disturb, opt-out)。

4. **`promotion` / `promotion-service`** (クーポン・タイムセール)
   - クーポン発行・割当・適用。発行枚数制限は Redis の `INCR + EXPIRE` で管理。
   - **タイムセール在庫**は `inventory-service` の予約 API を再利用。

5. **`order-saga-coordinator`** (新サービス)
   - Spring Statemachine もしくは Temporal クライアントで注文状態を管理。
   - 既存の `order-service` から「他サービスとのオーケストレーション責務」を抽出。

#### 【Phase 2 — 体験差別化】

6. **`search` / `search-service`**
   - OpenSearch + ベクトル検索 (Embedding は商品説明から `text-embedding-3-large` 相当で生成)。
   - **ハイブリッド検索** (BM25 + Vector) でロングテールにも強い検索体験を提供。
   - 商品 PG → Debezium → Kafka → Indexer。

7. **`recommendation` / `recommendation-service`**
   - 「あなたへのおすすめ」「この商品を見た人は」「カート補完」の 3 ユースケース。
   - 学習はオフライン (Iceberg + Spark / SageMaker)、推論はオンライン (gRPC + Redis Feature Cache)。
   - **Cold-start** は人気度ベース、ユーザー履歴蓄積後に協調フィルタ + シーケンシャル推薦 (Transformer4Rec) へ移行。

8. **`review` / `review-service`**
   - 投稿・モデレーション (LLM による NSFW / 誹謗中傷検知)。
   - 集計値はマテビュー or Redis Sorted Set。

9. **`cart` / `cart-service`** (現状はクライアント側 or 暗黙)
   - ゲスト → ログインのマージ、複数デバイス同期。
   - Redis (Hash) を一次ストレージ、Postgres にスナップショット。

#### 【Phase 3 — マルチテナント・グローバル化】

10. **`seller` / `seller-service`** (出品者管理、マーケットプレイス化への布石)
11. **`settlement` / `settlement-service`** (出品者向け売上精算)
12. **`tax` / `tax-service`** (国別税率、適格請求書)
13. **`fraud` / `fraud-service`** (機械学習ベース不正検知、決済前に同期チェック)

#### 【Phase 4 — プラットフォーム化】

14. **`policy` / `policy-service`** (OPA / Cedar による認可の中央集権化)
15. **`analytics-bff`** (社内向け BI 用 GraphQL/gRPC)
16. **`developer-portal`** (パートナー向け公開 API + OAuth 2.1)

### 4.3 共通ライブラリの拡張

- `common-observability`: OpenTelemetry SDK 初期化、MDC への traceId/spanId 注入。
- `common-saga`: Outbox テーブル DDL ジェネレータ + リレー実装。
- `common-idempotency`: HTTP/gRPC インターセプタで `Idempotency-Key` を強制。
- `common-feature-flag`: OpenFeature + Unleash アダプタ。

---

## 5. 横断的関心事 (Cross-cutting)

### 5.1 セキュリティ

- **認証**: OAuth 2.1 (Authorization Code + PKCE)、リフレッシュトークン回転。
- **認可**: gateway で粗粒度 (RBAC)、サービス内で細粒度 (ABAC via OPA)。
- **mTLS**: サービス間通信は Istio もしくは Linkerd の自動 mTLS。
- **シークレット**: HashiCorp Vault または AWS Secrets Manager。`jwt.secret` の yaml 直書きを即時撤廃。
- **WAF / Bot 対策**: Cloudflare WAF + Turnstile、OWASP CRS。
- **PII 暗号化**: 列レベル暗号化 (`pgcrypto`) と KMS キーリング。
- **監査ログ**: 認証イベントは別系統の append-only ログ (S3 Object Lock) に保存。

### 5.2 可観測性 (Observability)

- **Tracing**: OpenTelemetry → Tempo / Jaeger。gRPC + Kafka headers 経由で trace 伝播。
- **Metrics**: Micrometer → Prometheus → Grafana。RED / USE メトリクスを SLO ダッシュボードで可視化。
- **Logging**: Logback JSON → Loki (or OpenSearch)。`traceId` を MDC で全ログに付与。
- **Profiling**: Pyroscope (continuous profiling)、JVM では async-profiler 統合。
- **SLO / Error Budget**: Sloth で定義、Grafana でアラート。

### 5.3 信頼性パターン

- **Bulkhead** (Resilience4j) で gateway → 各サービスのスレッドプールを分離。
- **Circuit Breaker** で下流障害の伝播を遮断。
- **Retry** は指数バックオフ + ジッター、冪等な操作のみ。
- **Outbox + 冪等鍵** で at-least-once を吸収。
- **Dead Letter Topic** + 手動再処理 UI を `notification` / `inventory-event` に必須化。

### 5.4 データ管理

- **マイグレーション**: Flyway。`ddl-auto: validate` に固定。
- **CDC**: Debezium (Postgres logical replication)。
- **DWH**: S3 + Apache Iceberg、クエリは Trino。
- **個人データ**: GDPR/APPI に基づき「忘れられる権利」用の Soft-delete + Tombstone Topic。

---

## 6. デプロイメント計画

### 6.1 環境戦略

| 環境 | 目的 | データ |
|---|---|---|
| `local` | 開発者ワークステーション | `container-compose.yaml`、`ddl-auto: create` 維持可 |
| `dev` | 共有開発、PR プレビュー | Ephemeral namespace per PR (vcluster) |
| `staging` | 本番同等構成、E2E / 負荷試験 | 仮名化された本番スナップショット |
| `prod` | 本番 | マルチ AZ、Multi-Region (Phase 4) |

### 6.2 コンテナ & オーケストレーション

- **イメージビルド**: Jib (Spring Boot 公式推奨)。distroless ベース。
- **オーケストレーション**: Kubernetes (EKS / GKE)。
- **マニフェスト**: Helm + Kustomize overlays (環境差分)。
- **GitOps**: Argo CD で `prod` への同期は手動 sync、`dev`/`staging` は自動 sync。
- **プログレッシブデリバリー**: Argo Rollouts による Canary (5% → 25% → 100%)、メトリクス監視で自動ロールバック。

### 6.3 CI/CD パイプライン

```
PR open
  └─► Lint (ktlint, detekt) → Build → Unit Test
       └─► Container build (Jib) → Trivy SCA → SBOM (Syft)
            └─► Ephemeral Env Deploy (vcluster) → Smoke + Contract Test (Pact)
                 └─► Manual approval → Canary to staging → 負荷試験 (k6)
                      └─► Tagged release → Argo CD Sync → prod
```

### 6.4 データストアの本番構成

| ストア | 構成 |
|---|---|
| Postgres | AWS Aurora PG, Writer 1 + Reader 2, Multi-AZ, PITR 7 日 |
| Redis | ElastiCache (Cluster mode), 3 shard × 2 replica |
| Kafka | MSK (3 broker) + Schema Registry (Avro/Protobuf) |
| OpenSearch | 3 master + 6 data node, Hot-Warm 階層 |
| Object Storage | S3 (商品画像 / バックアップ / Iceberg) |

### 6.5 マルチリージョン (Phase 4)

- **Active-Active for read** (CDN + Edge KV)。
- **Active-Passive for write**: プライマリリージョンで集中、フェイルオーバー時は RPO ≤ 5min を Aurora Global Database で担保。
- **データ主権**: 日本ユーザーのデータは ap-northeast-1、韓国ユーザーは ap-northeast-2 にピン留め。

---

## 7. 主要な技術的チャレンジと解決方針

### 7.1 在庫オーバーセル防止
- **Optimistic Lock + Redis 分散ロック** (既存 `DistributedLock`) で短時間ロック。
- 高頻度商品のみ **Redis 在庫キャッシュ + Lua スクリプトでアトミック減算**、定期的に Postgres へリコンサイル。
- ピーク時 (タイムセール) は **キューイング (Redis Stream)** で受付順に直列化。

### 7.2 注文・決済・在庫の三者整合性
- **Saga (オーケストレーション)**:`order-saga-coordinator` が状態機械で管理。
- **補償処理**:在庫予約解除、決済キャンセル/返金、注文ステータスのロールバック。
- **タイムアウト管理**:各ステップに SLA を設定し、超過時は自動補償。

### 7.3 ホットキー / 人気商品問題
- 商品詳細を **Caffeine (L1) + Redis (L2) + CDN (L3)** の多段キャッシュ。
- キャッシュキーには `version` を含め、更新時は **Cache-Aside の書き込み無効化**。

### 7.4 イベント順序保証
- Kafka パーティションキーを **`aggregateId` (orderId / inventoryId)** で固定し、同一エンティティのイベント順序を保証。

### 7.5 メッセージ重複
- 既存 `IdempotentEventProcessor` を全 consumer で必須化。冪等鍵 TTL は処理 SLA × 3。

### 7.6 スキーマ進化
- Protobuf / Avro + Schema Registry。**後方互換のみ許容** ルールを CI で強制 (`buf breaking`)。

### 7.7 デプロイのダウンタイムゼロ化
- DB マイグレーションは **Expand → Migrate → Contract** の三段階。
- gRPC スタブは N と N+1 を同時にサポートする期間を 1 スプリント設ける。

### 7.8 開発者体験 (DevEx)
- **Tilt** + **vcluster** でローカル K8s 上に依存サービスをワンコマンド起動。
- **Backstage** で各サービスのオーナー・SLO・ランブックを集約。

---

## 8. 採用すべき先端技術

| カテゴリ | 技術 | 採用理由 |
|---|---|---|
| AI 検索 | OpenSearch k-NN + Embedding (例: voyage-3 / text-embedding-3-large) | ロングテール検索の精度向上 |
| 生成 AI | Claude (Anthropic) を `recommendation` / `review-moderation` / `customer-support-bot` に組み込み | 推薦理由生成、レビュー検閲、CS 一次対応 |
| ベクトル DB | pgvector (小規模) → Qdrant / Vespa (大規模) | 商品・ユーザーの類似度検索 |
| ワークフロー | Temporal | 長時間 Saga、配送追跡、リトライポリシーの宣言的記述 |
| サービスメッシュ | Istio Ambient (Sidecar-less) | mTLS / 認可ポリシー / Tracing を透過的に注入、コスト削減 |
| Feature Flag | OpenFeature + Unleash | 漸進的リリースと A/B テスト |
| Continuous Profiling | Pyroscope | 本番でも常時プロファイル取得 |
| Rust 化 | 一部のホットパス (在庫減算、価格計算) を WebAssembly 化して JVM から呼出 | レイテンシ最小化 (将来検討) |
| eBPF | Cilium による L7 認可・可観測性 | カーネルレベルの低オーバーヘッド計測 |
| データ基盤 | Apache Iceberg + Trino + dbt | レイクハウス化、分析と ML 特徴量を統合 |
| MLOps | Feast (Feature Store) + MLflow + Seldon Core | 推薦モデルの継続学習・サービング |
| エッジ | Cloudflare Workers + KV | パーソナライズされた CDN レンダリング (商品 OG 画像、AB バナー) |
| FinOps | OpenCost + Karpenter | コスト可視化 + Spot 活用で 30〜50% 削減 |

---

## 9. ロードマップ (四半期粒度)

| 期間 | フェーズ | 主要マイルストーン |
|---|---|---|
| **Q2 2026 (現在〜)** | Phase 0: 基盤強化 | Flyway 導入、Secrets 外出し、OpenTelemetry 統合、CI/CD 最低限、Helm チャート、`client-redis` の jitpack 依存解消 |
| **Q3 2026** | Phase 1a: 取引完結 | `payment-service`, `order-saga-coordinator`, `notification-service` 投入。MVP として「注文 → 決済 → 通知」が一気通貫で動く |
| **Q4 2026** | Phase 1b: 物流 + 販促 | `shipping-service`, `promotion-service`, `cart-service` 投入。クローズドβ開始 |
| **Q1 2027** | Phase 2: 体験差別化 | `search-service` (ハイブリッド), `recommendation-service`, `review-service`。一般公開 (GA) |
| **Q2 2027** | Phase 3: マルチテナント | `seller-service`, `settlement-service`, `fraud-service`, `tax-service`。マーケットプレイス化 |
| **Q3〜Q4 2027** | Phase 4: プラットフォーム化 | マルチリージョン化、`policy-service` (OPA)、`developer-portal`、生成 AI 機能の本格展開 |

---

## 10. リスクレジスター (上位 5)

| # | リスク | 影響 | 緩和策 |
|---|---|---|---|
| 1 | 現行 `ddl-auto: create` のまま本番デプロイされる | データ全損 | Phase 0 完了まで本番化を凍結、CI で `validate` 以外を reject |
| 2 | `jwt.secret` がリポジトリに残存 | アカウント全乗っ取り | 即時ローテート + git history 書き換え + Secrets Manager 移行 |
| 3 | Outbox リレーが単一障害点になる | イベント全停止 | リーダー選出 (ShedLock) + 多重起動可な設計 + Lag 監視 |
| 4 | Saga コーディネータの状態 DB 障害 | 注文不可 | Aurora Multi-AZ + Temporal などマネージド採用も検討 |
| 5 | 推薦・検索の AI コストが青天井 | 利益率毀損 | キャッシュ層 + 埋め込みの再利用 + バッチ事前生成 + コストアラート |

---

## 11. 体制とオーナーシップ (推奨)

| チーム | 担当サービス |
|---|---|
| Platform | `common*`, observability, CI/CD, K8s |
| Identity | `auth`, `user`, `policy` |
| Catalog | `product`, `search`, `recommendation`, `review` |
| Commerce | `cart`, `order`, `order-saga-coordinator`, `promotion` |
| Fulfillment | `inventory`, `shipping`, `inventory-event` |
| Financial | `payment`, `settlement`, `tax`, `fraud` |
| Growth | `notification`, `analytics-bff`, `developer-portal` |

各チームは **オンコール輪番**、**SLO**、**ランブック**、**サービスカタログ (Backstage)** を必須で保有する。

---

## 12. 次のアクション (今週中に着手すべきこと)

1. `jwt.secret` の即時ローテートと環境変数化 (PR を切る)。
2. Flyway をいずれか 1 サービス (`product-service` 推奨) に導入し、テンプレート化。
3. OpenTelemetry Spring Boot Starter を `user-api-gateway` に投入し、Jaeger をローカル `container-compose` に追加。
4. `payment-service` のドメインモデル (DDD ワークショップ) を 1 日で固める。
5. 本ドキュメントをチームレビューにかけ、**Phase 0 のスコープ凍結**と **Phase 1 のチーム配置**を確定。

---

> 本計画は生きたドキュメントである。Phase 完了ごとに振り返りを行い、ロードマップ・モジュール構成・採用技術を継続的に更新すること。
