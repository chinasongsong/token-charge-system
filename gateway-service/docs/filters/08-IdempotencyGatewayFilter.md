# IdempotencyGatewayFilter（O-10）

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.gateway.infrastructure.web.IdempotencyGatewayFilter` |
| Order | `HIGHEST_PRECEDENCE + 12`（在 API Key 解析 +11 之后） |
| 路径 | 仅 `POST /v1/chat/completions` |

---

## 1. 背景

`X-Trace-Id` 用于**日志关联**；若同时作为**扣费幂等键**，客户端重试不带头时会每次生成新 UUID，导致 `settle` 重复扣款。

O-10 引入 **`X-Idempotency-Key`**（客户端 UUID v4），网关合成 **`X-Idempotency-Key-Composite`** 传给下游，billing 以复合键为 `request_orders.idempotency_key`。

---

## 2. 作用

| 步骤 | 行为 |
|------|------|
| 客户端提供 `X-Idempotency-Key` | 校验 UUID v4；复合键 `userId:apiKeyId:clientKey`；来源 `CLIENT` |
| 未提供 | 复合键 `userId:apiKeyId:traceId`；来源 `TRACE_ID_FALLBACK`（**不推荐生产依赖**） |
| 仅 JWT（无 `X-Api-Key-Id`） | `apiKeyId` 使用占位 `0`：`userId:0:clientKey` |
| 无 `X-User-Id` | 跳过（无法合成） |

注入头：

- `X-Idempotency-Key-Composite`
- `X-Idempotency-Source`（`CLIENT` | `TRACE_ID_FALLBACK`）

---

## 3. 与 Trace 的分工

| 头 | 用途 |
|----|------|
| `X-Trace-Id` | 观测、错误 JSON、订单 `trace_id` 字段 |
| `X-Idempotency-Key` / Composite | **结算幂等**（`idempotency_key`） |

**客户端重试**：应固定 `X-Idempotency-Key`；`X-Trace-Id` 可相同或每次新建，但**扣费以复合幂等键为准**。

---

## 4. 全链路（改造后）

```mermaid
sequenceDiagram
  participant C as Client
  participant GW as Gateway
  participant AD as adapter
  participant BI as billing
  C->>GW: POST /v1/chat/completions<br/>X-Idempotency-Key + X-Trace-Id
  GW->>GW: Trace + Key + Idempotency
  GW->>AD: 转发复合头
  AD->>BI: settle(traceId, idempotencyKey, source)
```

---

## 5. 错误码

| HTTP | code | 场景 |
|------|------|------|
| 400 | `I400001` | `X-Idempotency-Key` 非 UUID v4 |

---

## 6. 客户端幂等键「出问题」时会怎样？

| 客户端行为 | 网关 | 是否可能重复扣费 |
|------------|------|------------------|
| **未传** `X-Idempotency-Key` | `TRACE_ID_FALLBACK`（复合键含当次 `traceId`） | **会**。重试若换新 `X-Trace-Id` 且无幂等键 → 新单 |
| **格式非法**（非 UUID v4） | `400` / `I400001`，**不进 adapter** | **不会**（请求被拒） |
| **每次重试生成新 UUID**（SDK bug） | 每次新复合键 | **会**。与没传幂等键类似 |
| **两次不同业务共用同一 Key** | 第二次 `settle` 命中已完成订单 → 直接 return | **不会重复扣**；但第二次可能仍调了模型（adapter 不拦） |
| **正确：同一逻辑请求固定同一 Key** | 复合键不变 | **不会**（幂等生效） |

结论：**幂等键只能防「同 key 的重复结算」**，不能代替客户端正确生成/复用 Key。生产环境应强制 SDK 在「一次用户意图」内固定 `X-Idempotency-Key`；不要依赖 `TRACE_ID_FALLBACK`。

---

## 7. 相关文档

- [01-TraceGatewayFilter.md](./01-TraceGatewayFilter.md)
- [09-错误响应与头约定.md](../09-错误响应与头约定.md)
- adapter：[05-BillingSettlementClient.md](../../../adapter-service/docs/components/05-BillingSettlementClient.md)
