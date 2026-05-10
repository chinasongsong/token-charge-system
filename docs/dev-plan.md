# 开发计划单（执行路线图）

> 本文是 Agent 后续按阶段执行的**唯一权威路线图**。所有阶段必须在通过验收标准后才推进到下一阶段。
> 上游依据：`plan.md`（产品/PRD）、`ARCHITECTURE.md`、`docs/architecture/LAYERS.md`、`docs/SECURITY.md`、`AGENTS.md`。

## 0. 全局约定

- **技术栈**：JDK 21 + Spring Boot 3.3 + Spring Cloud Gateway + Spring WebFlux/MVC（按服务选用）+ MyBatis-Plus + Redis + MySQL 8.0 + RabbitMQ（P7 引入）。前端 Vue 3 + Vite + TypeScript + Pinia + Element Plus。
- **构建**：Maven 多模块；根 `pom.xml` 管理 BOM 与版本；每个服务独立 `Dockerfile`。
- **目录强约束**：每个 Java 服务必须严格分 `presentation / application / domain / infrastructure` 四层（见 `LAYERS.md`），并通过 `scripts/check_boundaries.py` 守护。
- **每阶段闭环动作**：
  1. 写代码 / 文档 / SQL。
  2. 跑 `python scripts/check_boundaries.py` 与 `python scripts/gc_scan.py`。
  3. 跑该阶段单元/集成测试。
  4. 同步更新 `ARCHITECTURE.md`、`LAYERS.md`、`SECURITY.md`（若有结构性/安全性变化）。
  5. 在本文件相应阶段勾选 `Done` 并记录交付物路径。

## 1. 默认假设（评审通过后写死）

| 项 | 默认值 |
|---|---|
| 首发用户场景 | 开发者 API + 普通用户 Web 对话 |
| 首批供应商 | 阿里云百炼（DashScope OpenAI 兼容）、智谱 AI（GLM OpenAI 兼容） |
| 首期支付通道 | Mock 支付（跑通账务闭环），后接微信/支付宝 |
| 前端框架 | Vue 3 + Vite + TypeScript + Pinia + Vue Router + Element Plus |
| 消息中间件 | RabbitMQ（P7 引入；P0–P6 走同步调用 + 数据库事件表过渡） |
| 部署 | Docker Compose（开发/测试），生产预留 K8s |

## 2. 阶段总览

| 阶段 | 名称 | 周期 | 关键产出 | 对齐 plan.md |
|---|---|---|---|---|
| **P0** | 工程地基 | W1 | Maven 多模块、Compose 全量基础设施、初始化 SQL、OpenAPI 骨架、统一返回与错误码 | §2、§4、§7 W1-W2 |
| **P1** | 用户与认证 | W2 | `user-center-service`：注册/登录/JWT/密码重置、`users`/`user_devices` 表 | §11.4、§14.1 |
| **P2** | 网关 + 首家供应商 | W2-W3 | `gateway-service`、`adapter-service`（DashScope）、`/v1/chat/completions` 非流式 | §4、§7 W1-W2 |
| **P3** | 计费 MVP | W3-W4 | `billing-service`：API Key、余额、`request_orders`+`usage_ledger`、Redis 限流 | §11.4、§14.2/14.5/14.6 |
| **P4** | 第二家供应商 + 路由 | W4 | 智谱适配、路由策略、熔断/重试/降级 | §7 W3-W4 |
| **P5** | 支付与套餐 | W5-W6 | `payment-service`：Mock 支付 + 充值下单 + 回调幂等；`pricing_plans` + `user_subscriptions`；退款/发票 | §14.3/14.7 |
| **P6** | C 端控制台 | W5-W7 | `console-web`（Vue3）：登录、Dashboard、Key 管理、充值套餐、账单、Playground | §11.3、§12 |
| **P7** | 运营后台 + 风控 + 可观测 | W7-W9 | `ops-console`：供应商/价格/风控/告警；Prometheus + Grafana；引入 RabbitMQ（请求日志/风控/账务三类队列） | §4、§8.1 |
| **P8** | SSE 流式 + 客服 + 验收 | W9-W12 | SSE 稳定化、`support_tickets`、压测、文档中心、SLA 分级 | §8、§9、§11.7 |

---

## 3. 阶段详情

### P0 工程地基（必须最先完成）

**任务**
- [x] P0-1 创建根 `pom.xml`（packaging=pom，统一 Spring Boot/Cloud BOM、Java 21、编码、插件）。
- [x] P0-2 建立模块结构：
  - `common/` (`common-core`、`common-web`、`common-security`、`common-mybatis`)
  - `gateway-service/`、`user-center-service/`、`adapter-service/`、`billing-service/`、`payment-service/`、`ops-console/`
  - `console-web/`（Vue 3 + Vite + TS，独立 `package.json`）
- [x] P0-3 每个 Java 服务初始化四层包结构 `com.tokenhub.<svc>.{presentation,application,domain,infrastructure}` + `Application.java` 启动类。
- [x] P0-4 在 `common-core` 定义统一返回 `ApiResponse<T>`、统一错误码 `ErrorCode`、`BusinessException`、`TraceId` 拦截器。
- [x] P0-5 在 `common-web` 定义 `GlobalExceptionHandler`、`RequestLoggingFilter`。
- [x] P0-6 在 `common-security` 定义 JWT/ApiKey 工具类（仅工具，不耦合业务）。
- [x] P0-7 各服务 `Dockerfile`（多阶段构建：`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`）。
- [x] P0-8 扩展 `deploy/docker-compose.yml`：加入 `rabbitmq`、`prometheus`、`grafana` 占位段（先注释，按阶段启用）；应用服务镜像引用先注释。
- [x] P0-9 初始化 SQL：`deploy/sql/V1__core_schema.sql`（users / user_devices / api_keys / account_balance / request_orders / usage_ledger / payment_orders / pricing_plans / user_subscriptions / model_providers / model_prices / risk_events / support_tickets）。
- [x] P0-10 OpenAPI 契约骨架：`docs/openapi/unified-api.yaml`（先列接口路径与请求/响应 Schema 头部）。
- [x] P0-11 更新 `ARCHITECTURE.md` 模块清单与依赖关系图；更新 `LAYERS.md` 模块边界表。
- [x] P0-12 跑 `scripts/check_boundaries.py` 与 `gc_scan.py` 全绿。

**P0 交付物路径**：根 `pom.xml`；各 Java 模块与 `console-web/`；共享库：`common/*/src/main/java`；SQL：`deploy/sql/V1__core_schema.sql`；契约：`docs/openapi/unified-api.yaml`；Compose：`deploy/docker-compose.yml`。

**验收**
- 根目录 `mvn -B -ntp -DskipTests package` 成功。
- `docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d mysql redis` 可用。
- 边界检查脚本通过；CI 全绿。

---

### P1 用户与认证（user-center-service）

**任务**
- [ ] P1-1 `users`/`user_devices` MyBatis-Plus Entity + Mapper。
- [ ] P1-2 注册/登录/登出 + 密码 BCrypt + JWT 颁发（`common-security` 提供原子能力）。
- [ ] P1-3 邮箱验证码（先打印到日志，留邮件 SPI 接口）。
- [ ] P1-4 找回/重置密码。
- [ ] P1-5 设备指纹与登录轨迹落库。
- [ ] P1-6 接口：`POST /user/register`、`POST /user/login`、`POST /user/password/forgot`、`POST /user/password/reset`、`GET /user/me`。
- [ ] P1-7 单元测试覆盖 application 层用例 ≥ 80%。

**验收**
- 用 curl 走完注册→登录→拿 JWT→访问 `/user/me`。
- `domain` 层无 Spring/MyBatis 依赖（边界检查通过）。

---

### P2 网关 + 首家供应商（gateway-service + adapter-service）

**任务**
- [ ] P2-1 `gateway-service` 引入 Spring Cloud Gateway（WebFlux），路由 `/v1/**` → `adapter-service`，`/user/**` → `user-center-service`。
- [ ] P2-2 全局过滤器：TraceID、JWT 解析（用户调用）、API Key 解析（开发者调用）。
- [ ] P2-3 `adapter-service` 定义 `ProviderAdapter` 接口（`chat`, `embedding`, `listModels`）。
- [ ] P2-4 实现 `DashScopeAdapter`（OpenAI 兼容协议，先非流式）。
- [ ] P2-5 错误码统一：把供应商错误码归一到平台错误码（在 `adapter-service` 内）。
- [ ] P2-6 `model_providers` 表配置驱动，密钥从环境变量加载。
- [ ] P2-7 接口：`POST /v1/chat/completions`（非流式）、`GET /v1/models`。

**验收**
- 用 OpenAI Python SDK，仅替换 base_url 即可调通。
- 故意填错 API Key/JWT 时，返回平台统一错误码。

---

### P3 计费 MVP（billing-service）

**任务**
- [ ] P3-1 `api_keys` Entity + 创建/列表/禁用接口（`POST/GET/PATCH /apikeys`）。
- [ ] P3-2 `account_balance` 余额账户 + 充值/扣费 Domain Service（用乐观锁/悲观锁防超卖）。
- [ ] P3-3 `request_orders`+`usage_ledger` 双写（同事务）。
- [ ] P3-4 Redis 限流：`api_key:{id}:qps`、`api_key:{id}:daily`，过滤器在网关层。
- [ ] P3-5 计费规则引擎读取 `model_prices`（按 input/output token 计价）。
- [ ] P3-6 `GET /v1/usage`、`GET /dashboard/summary`、`GET /billing/orders`。
- [ ] P3-7 计费幂等：`usage_ledger.idempotency_key = trace_id`。

**验收**
- 调一次模型 → 余额扣减 → `request_orders`/`usage_ledger` 各 1 行 → `/dashboard/summary` 数据一致。
- 余额不足时返回 `BALANCE_INSUFFICIENT` 错误码并阻断。

---

### P4 第二家供应商 + 路由

**任务**
- [ ] P4-1 `ZhipuAdapter` 实现。
- [ ] P4-2 `RoutingPolicy` 接口 + `WeightedRoutingPolicy`（按可用率/成本/延迟权重）。
- [ ] P4-3 Resilience4j 熔断 + 重试 + 超时。
- [ ] P4-4 故障切换：A 失败 → 自动切 B；记录 `risk_events` 类型 `provider_failover`。
- [ ] P4-5 模型路由测试（混沌测试：手动让 A 返回 5xx）。

**验收**
- 关掉 A 的密钥，调用仍成功，账单 provider 字段为 B。

---

### P5 支付与套餐（payment-service）

**任务**
- [ ] P5-1 `pricing_plans` + `user_subscriptions` Entity 与 CRUD。
- [ ] P5-2 `payment_orders` 下单接口 `POST /billing/recharge`、`POST /billing/subscribe`。
- [ ] P5-3 Mock 支付回调端点 + 签名校验骨架（生产替换为微信/支付宝）。
- [ ] P5-4 回调幂等（基于 `order_no` + Redis 锁）+ 失败补偿任务（定时拉单）。
- [ ] P5-5 退款 `POST /billing/refund/apply` + 审核状态机。
- [ ] P5-6 `invoices` 开票占位（先生成 PDF 编号，不接真实税控）。
- [ ] P5-7 日终对账定时任务（与三方账单比对）。

**验收**
- Mock 支付 → 余额到账 → 重复回调不重复加钱（幂等通过）→ 退款流程闭环。

---

### P6 C 端控制台（console-web）

**任务**
- [ ] P6-1 Vue 3 + Vite + TS 工程初始化（Pinia、Vue Router、Element Plus、Axios、UnoCSS 或 Tailwind 视情况）。
- [ ] P6-2 设计 token：颜色/字号/间距对齐 `plan.md §15.3`，封装为 SCSS 变量与 ElementPlus 主题覆写。
- [ ] P6-3 公共能力：Layout、Sidebar、TopBar、路由守卫（基于 JWT）、Axios 拦截器（自动带 Token、统一错误码处理）。
- [ ] P6-4 页面：登录/注册、Dashboard、API Keys、Playground、Billing（充值/套餐/账单）、Errors、Tickets、Account。
- [ ] P6-5 Playground 支持 SSE 流式输出（基于 `EventSource` 或 fetch+ReadableStream，先非流式占位，P8 切流式）。
- [ ] P6-6 Light/Dark 双主题（ElementPlus dark 模式 + 自定义 token）。
- [ ] P6-7 多阶段构建 Dockerfile（node 构建 → nginx 托管），接入 Compose。

**验收**
- 按 `plan.md §11.2` 路径 A/B 端到端走通；Lighthouse 移动端性能 ≥ 80。

---

### P7 运营后台 + 风控 + 可观测

**任务**
- [ ] P7-1 `ops-console` 服务：供应商管理（CRUD + 启停）、价格配置、路由策略、风控规则、告警阈值、审计日志查询。
- [ ] P7-2 在 `console-web` 增加 `/admin/**` 路由（基于角色 RBAC）。
- [ ] P7-3 风控规则引擎：登录异地/高频、调用频控、内容安全（接 Mock 审核 SPI）。
- [ ] P7-4 接入 Micrometer + Prometheus + Grafana（基础 Dashboard：QPS、P95、错误率、余额存量）。
- [ ] P7-5 引入 RabbitMQ（请求日志、风控事件、账务事件三类队列），生产端在网关/计费侧异步投递，消费端在 ops/audit 服务订阅。
- [ ] P7-6 审计日志 append-only 表 + 导出能力。

**验收**
- Grafana 看到至少 4 个核心指标实时；触发风控规则可在 `risk_events` 与告警中心可见。

---

### P8 SSE 流式 + 客服 + 验收

**任务**
- [ ] P8-1 `gateway` SSE 透传（`text/event-stream`）+ 心跳 + 断流重试。
- [ ] P8-2 流式 token 计量回填（按 chunk 累加，结束后写 `request_orders`）。
- [ ] P8-3 `support_tickets` 工单系统（`POST/GET /support/tickets`）+ 客服回复 + 赔付。
- [ ] P8-4 公告与活动券（运营后台配置）。
- [ ] P8-5 SLA 分级：会员等级 → QPS/优先级队列映射。
- [ ] P8-6 文档中心（API 文档 + SDK 示例 + 错误码）。
- [ ] P8-7 全链路压测（k6 或 JMeter）：流式/非流式成功率 ≥ 99%。
- [ ] P8-8 运行 `plan.md §9` MVP 验收清单。

**验收**
- MVP 验收清单全部勾选；CI/边界检查/GC 扫描全绿；可在 Compose 上一键拉起整套系统。

---

## 4. 跨阶段必做事项（每个阶段都要带）

- 单元/集成测试（JUnit 5 + Mockito + Testcontainers）。
- 安全基线复核（密钥不入库、敏感字段脱敏、TraceID）。
- 文档同步：`ARCHITECTURE.md`、`LAYERS.md`、`SECURITY.md`、`docs/openapi/unified-api.yaml`、本计划单的 Done 勾选。
- 边界检查 + GC 扫描必须绿色。

## 5. 风险登记

| 风险 | 应对 |
|---|---|
| 供应商协议变化 | adapter 层契约测试 + 版本隔离 |
| MVP 期间引入过重的中间件（Kafka 等） | P7 才引入，前期用 Redis Stream |
| 支付集成阻塞主流程 | P5 先 Mock 通账务，再接真实通道 |
| 前后端进度耦合 | OpenAPI 契约先行（P0 完成骨架），前端基于 mock 并行 |
| 边界违规导致 CI 红 | 每个 PR 跑边界检查；棘轮基线只能减不能增 |

## 6. 当前阶段

- **已完成**：P0 工程地基（验收以 CI JDK 21 + `mvn` / Compose 冒烟为准）。
- **下一步**：开始 **P1 用户与认证**（`user-center-service` 注册登录与 JWT）。
