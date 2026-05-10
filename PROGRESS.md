# 阶段交付总结

> **强制**：每完成 `docs/dev-plan.md` 中的一个阶段（P0–P8），必须在**同一提交或紧随其后的提交**中更新本文件对应章节。详见 [`AGENTS.md`](./AGENTS.md)「阶段交付总结（强制）」。
>
> **写法**：用完整句子说明本阶段**做了什么**、**关键路径/模块**、**如何验收**、**遗留或下一阶段的输入**。避免只写「已完成」而无实质信息。

---

## P0 工程地基

**完成状态**：已完成  

### 本阶段做了什么

- 建立 Maven 多模块（根 BOM、JDK 21 Enforcer）、公共库 `common-core` / `common-web` / `common-security` / `common-mybatis` 与各业务服务空壳（四层包 + `Application`）。
- 统一 HTTP 信封 `ApiResponse`、错误码、Trace、`GlobalExceptionHandler`、JWT/ApiKey 工具类；Compose 挂载 `deploy/sql`；提供 `V1__core_schema.sql`、OpenAPI 骨架、`console-web`（Vue3+Vite）脚手架。
- 各服务多阶段 `Dockerfile`；脚本 `check_boundaries.py`、`gc_scan.py` 与 CI 可对齐。

### 关键交付物（路径）

根 `pom.xml`、`common/**`、`gateway-service` / `user-*` / `adapter-*` / `billing-*` / `payment-*` / `ops-console`、`console-web/`、`deploy/sql/V1__core_schema.sql`、`deploy/docker-compose.yml`、`docs/openapi/unified-api.yaml`。

### 验收与检查

- `mvn package`（JDK 21）；Compose 起 `mysql`/`redis`；边界检查与 GC 扫描通过。

### 遗留 / 下一阶段的输入

- 业务接口待 P1 起在各服务落地；网关路由待 P2。

---

## P1 用户与认证（user-center-service）

**完成状态**：已完成  

### 本阶段做了什么

- `users`、`user_devices`、`password_reset_codes`（`V2`）与 MyBatis-Plus 仓储实现；领域层无框架依赖。
- `UserApplicationService`：注册/登录/JWT（HS256，`JWT_SECRET`）、BCrypt、忘记/重置密码（验证码 SHA-256 落库 + 日志模拟邮件）、登录设备轨迹写入 `user_devices`。
- `GET /user/me` 走 MVC 拦截器校验 Bearer JWT；`POST /user/register`、`/login`、`/logout`、`/password/forgot`、`/password/reset`。
- Application 层单测 + Jacoco 对 `UserApplicationService` 行覆盖率门禁；`GlobalExceptionHandler` 对齐 HTTP 状态码（如 401）。

### 关键交付物（路径）

`user-center-service/src/main/java`（`domain` / `application` / `infrastructure.persistence` / `presentation`）、`deploy/sql/V2__password_reset_codes.sql`、扩展后的 `docs/openapi/unified-api.yaml`、`user-center-service/src/main/resources/application.yml`。

### 验收与检查

- curl：注册 → 登录 → Bearer 访问 `/user/me`；`python scripts/check_boundaries.py` 通过。

### 遗留 / 下一阶段的输入

- 真实邮件通道替换 `VerificationMailPort`；网关统一入口待 P2。

---

## P2 网关 + 首家供应商（DeepSeek）

**完成状态**：已完成  

### 本阶段做了什么

- **`gateway-service`**：Spring Cloud Gateway 路由 `/v1/**` → 适配器、`/user/**` → 用户中心；HTTP 客户端超时；`TraceGatewayFilter`（`X-Trace-Id`）；`V1IngressAuthGatewayFilter`（`/v1/**` 非空 Bearer，可选与 `JWT_SECRET` 一致的 JWT 校验并注入 `X-User-Id`）；统一 JSON 错误体。
- **`adapter-service`**：`ProviderAdapter` + **`DeepSeekProviderAdapter`**（库表 `model_providers` 优先 `base_url`，否则 `DEEPSEEK_BASE_URL_FALLBACK`）；服务端密钥 `DEEPSEEK_API_KEY`；默认聊天模型 **`deepseek-v4`**；上游错误映射为 `BusinessException`；**`OpenAiCompatibleController`** 暴露 **`POST /v1/chat/completions`**、**`GET /v1/models`**；MyBatis 注册表与 **`V3__seed_model_providers_deepseek.sql`** 种子数据。
- **未纳入本阶段**：网关侧「开发者 API Key」解析与计费联动，顺延至 **P3**（与 `api_keys` 一致后再接）。

### 关键交付物（路径）

`gateway-service/src/main/resources/application.yml`、`gateway-service/src/main/java/.../infrastructure/web/*.java`、`adapter-service`（`application` / `domain/provider` / `infrastructure/deepseek` / `presentation/OpenAiCompatibleController.java`）、`deploy/sql/V3__seed_model_providers_deepseek.sql`、`deploy/env.example`。

### 验收与检查

- 启动 MySQL（含 `V1`–`V3` 初始化）、`user-center`（8101）、`adapter-service`（8102）、`gateway`（8080）；配置 **`DEEPSEEK_API_KEY`**。
- `curl http://localhost:8080/v1/models -H "Authorization: Bearer test"` 返回模型列表；`POST /v1/chat/completions`（JSON 与 OpenAI 兼容）经网关转发至适配器并调用上游。
- `mvn -pl gateway-service,adapter-service -am test`；`python scripts/check_boundaries.py` 通过。

### 遗留 / 下一阶段的输入

- 开发者 API Key 入站、扣费与用量：`billing-service` **P3**。

## P3 计费 MVP

**完成状态**：已完成  

### 本阶段做了什么

- 新增 **`billing-service`（默认 8103）**：MyBatis-Plus + `api_keys` / `account_balance`（乐观锁）/ `request_orders` + `usage_ledger` 同事务结算；**`model_prices`** 计价（按千 token **micro** 单价）；对外 **`POST/GET/PATCH /apikeys`**（JWT）、**`GET /dashboard/summary`**、**`GET /billing/orders`**、**`GET /v1/usage`**；内部接口 **`/internal/api-keys/by-fingerprint/{fp}`**、**`/internal/billing/preflight`**、**`/internal/billing/settle`**（`X-Internal-Token`）；开发可选 **`POST /billing/account/mock-deposit`**（`tokenhub.billing.allow-mock-deposit`）。
- **`deploy/sql/V4__seed_model_prices_deepseek_v4_flash.sql`**：为 `deepseek` 供应商写入 **`deepseek-v4-flash`** 价目占位。
- **`gateway-service`**：路由 **`/v1/usage`、`/apikeys`、`/dashboard`、`/billing` → billing**（须排在 **`/v1/**` 适配器路由之前**）；**`BillingApiKeyResolveGatewayFilter`**（不透明 Bearer → 指纹解析并注入 **`X-User-Id` / `X-Api-Key-Id`**）；**`BillingPreflightGatewayFilter`**（`POST /v1/chat/completions` 前余额预检，**402/B402001**）；**`ApiKeyRedisRateLimitGatewayFilter`**（每秒桶，配置 **`tokenhub.gateway.rate-limit-per-second`**）；依赖 **Redis**（`spring-boot-starter-data-redis-reactive`）。
- **`adapter-service`**：**`BillingSettlementClient`** 在 chat 返回后根据 **`usage`** 与网关头 **`X-Trace-Id`** 调用 billing **settle**（可关 **`BILLING_SETTLEMENT_ENABLED`**）。
- **`common-web`**：**`BALANCE_INSUFFICIENT`** 映射 **HTTP 402 Payment Required**。

### 关键交付物（路径）

`billing-service/src/main/java`（四层包）、`deploy/sql/V4__seed_model_prices_deepseek_v4_flash.sql`、`gateway-service/.../infrastructure/web/Billing*.java`、`ApiKeyRedisRateLimitGatewayFilter.java`、`adapter-service/.../billing/BillingSettlementClient.java`。

### 验收与检查

- 执行 **V4** SQL；启动 **MySQL、Redis、billing、adapter、gateway**；**JWT** 与 user-center 同 **`JWT_SECRET`**；网关与 billing 同 **`BILLING_INTERNAL_TOKEN`**。
- 用户 **mock-deposit**（若开启）→ 创建 **API Key** → 用 **sk_tokenhub_** 调 **`POST /v1/chat/completions`**（经 8080）→ 余额扣减、`request_orders`/`usage_ledger` 有记录 → **`GET /dashboard/summary`** 一致；余额为 0 时预检 **402**。
- `mvn -pl gateway-service,billing-service,adapter-service -am test`（或 `compile`）；`python scripts/check_boundaries.py`。

### 遗留 / 下一阶段的输入

- **日配额限流**、更精细的预扣款 / 流式中途截断计费等：可在 **P4/P6** 与运营策略一并演进。

---

## P4 第二家供应商 + 路由

**完成状态**：已完成（MVP）  

### 本阶段做了什么

- **`ZhipuProviderAdapter`**：OpenAI 兼容 `POST {base}/chat/completions`，库表优先智谱 `base_url`，否则 `tokenhub.adapter.zhipu-base-url-fallback`；密钥 **`ZHIPU_API_KEY`**；静态 **`GET /v1/models`** 列出 **`glm-4-flash`**。
- **`FailoverRoutingAdapter`**（`@Primary`）：主调 DeepSeek，可恢复类失败（`INTERNAL` / `TOO_MANY_REQUESTS`、网络/5xx）且智谱已配置密钥时重试智谱，并重写 **model** 为 **`glm-4-flash`**；**`RiskEventRecorder`** 写入 **`risk_events`**（`event_type=adapter_provider_failover`）。
- **`BillingSettlementClient`**：按请求体 **model** 启发式推断 **`providerCode`**（`glm*` → **zhipu**），以便 **`model_prices`** 命中。
- **`deploy/sql/V5__seed_model_providers_zhipu.sql`**：种子供应商 **zhipu** 与 **`glm-4-flash`** 价目占位。

### 关键交付物（路径）

`adapter-service/.../zhipu/ZhipuProviderAdapter.java`、`routing/FailoverRoutingAdapter.java`、`routing/AdapterRoutingConfiguration.java`、`risk/RiskEventRecorder.java`、`persistence/RiskEvent*.java`、`billing/BillingSettlementClient.java`、`deploy/sql/V5__seed_model_providers_zhipu.sql`。

### 验收与检查

- 执行 **V5**；配置可选 **`ZHIPU_API_KEY`**；**`ADAPTER_FAILOVER_ENABLED`**（默认 true）。主线路不可用时（或模拟）可切智谱；**`risk_events`** 有 failover 记录；`python scripts/check_boundaries.py` 通过。

### 遗留 / 下一阶段的输入

- 按模型或路由策略做显式供应商选择（非仅 failover）；流式 **`/v1/chat/completions` SSE** 仍待 P8。

---

## P5 支付与套餐

**完成状态**：已完成（Mock + 套餐 MVP；真实渠道仍待）  

### 本阶段做了什么

- **`billing-service`**：幂等入账 **`POST /internal/billing/credit`**；套餐与订阅占位、退款/发票申请接口、**`BillingReconciliationScheduler`** 对账开关占位；**`deploy/sql/V7__p5_p7_p8_extensions.sql`** 等扩展表。
- **`payment-service`（8104）**：**`POST /payments/mock/recharge`**（同步入账）；**`POST /payments/mock/checkout`**（`INIT` 订单）；**`POST /payments/mock/callback`**（HMAC-SHA256 **`MOCK_CALLBACK_SECRET`**、时间窗、订单与用户/金额校验）；**`PaymentCallbackOrderLock`**（Redis SET NX 或 JVM 短锁）与入账幂等；**`PaymentReconciliationScheduler`** 扫描 `INIT` 占位；**`spring-boot-starter-data-redis`** 与 **`POST /internal/payments/recharge`** 内部幂等键。
- **`gateway-service`**：**`/payments/**` → payment；**`/billing/**` → billing。

### 关键交付物（路径）

`deploy/sql/V6__balance_topup_receipts.sql`、`billing-service/.../InternalBillingController.java`、`AccountBalanceApplicationService.java`、`payment-service`（`MockPaymentApplicationService`、`MockPaymentController`、`BillingCreditClient`）、`gateway-service/application.yml`。

### 验收与检查

- 登录 JWT → **`POST /payments/mock/recharge`**（经 **8080**）→ **`GET /dashboard/summary`** 余额增加；重复 **`sourceRef`**（订单号）不重复入账。

### 遗留 / 下一阶段的输入

- 微信/支付宝真实渠道、渠道侧对账字段对齐；退款与发票审批流深化。

---

## P6 C 端控制台

**完成状态**：部分完成（MVP + 会话状态）  

### 本阶段做了什么

- **`console-web`**：Vite 代理 **`/api` → 网关 8080**；**Pinia** 会话（**`src/stores/session.ts`**）与 **`router.beforeEach`** JWT 路由守卫；**`ShellView`** 退出登录；**`src/api/client.ts`** 统一 **`ApiResponse`** 与 **`localStorage` Token**；路由 **登录 / 概览 / 充值 / 工单**。

### 关键交付物（路径）

`console-web/src/views/*.vue`、`src/stores/session.ts`、`src/router/index.ts`、`src/api/client.ts`、`vite.config.ts`、`.env.development`。

### 验收与检查

- **`npm run build`**；**`npm run dev`** 后登录 → 概览展示 **`/dashboard/summary`**；充值、工单（需网关与各服务已启动）。

### 遗留 / 下一阶段的输入

- API Key 管理页、Playground、设计 token 与设计稿对齐、生产 Dockerfile。

---

## P7 运营后台 + 风控 + 可观测

**完成状态**：部分完成  

### 本阶段做了什么

- **`ops-console`（8105）**：**`GET /ops/model-providers`**、**`GET /ops/audit-events`**（读 **`audit_events`**，可分页条数），请求头 **`X-Ops-Token`**（**`OPS_INTERNAL_TOKEN`**）。
- **网关**：**`/ops/**` → ops-console；**`management.endpoints.web.exposure`** 含 **`prometheus`**（网关侧指标端点，Grafana 大盘与其它服务拉取仍待统一）。
- 适配器故障转移、计费按模型推断 provider 已部分覆盖「风控/多供应商」目标。

### 关键交付物（路径）

`ops-console`（`OpsModelProviderController`、`OpsAuditEventController`、`OpsTokenMvcInterceptor`、`OpsWebConfiguration`）、`gateway-service/application.yml`（Prometheus）。

### 验收与检查

- **`curl -H "X-Ops-Token: dev-ops-token" http://localhost:8080/ops/model-providers`**；**`/ops/audit-events?limit=50`**；**`GET /actuator/prometheus`**（网关）；`python scripts/check_boundaries.py`。

### 遗留 / 下一阶段的输入

- 统一运营鉴权（OIDC/账号体系）、RabbitMQ 队列、Grafana 模板与告警、审计导出文件。

---

## P8 SSE 流式 + 客服 + 验收

**完成状态**：部分完成  

### 本阶段做了什么

- **`user-center-service`**：**`GET/POST /user/support/tickets`**；**`GET/POST /user/support/tickets/{id}/messages`**（**`support_ticket_messages`**，用户角色 **`USER`**）；**`SupportTicketMessageMapper`** 与预览字段更新。
- **`billing-service`**：**`BillingReconciliationScheduler`**（占位）；**SSE 流式**：仍以同步 **`/v1/chat/completions`** 与网关长超时为准，专用 `text/event-stream` 透传仍待。

### 关键交付物（路径）

`user-center-service/.../SupportTicket*.java`、`deploy/sql/V7__p5_p7_p8_extensions.sql`（工单消息表）。

### 验收与检查

- 登录后对工单发消息、拉取消息列表；`python scripts/check_boundaries.py` 与 `python scripts/gc_scan.py` 通过。

### 遗留 / 下一阶段的输入

- **SSE** 端到端、AGENT 回复与赔付、公告运营 API、压测与 MVP 验收清单 **`plan.md §9`**。
