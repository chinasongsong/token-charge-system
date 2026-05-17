# OpenAiCompatibleController

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.adapter.presentation.OpenAiCompatibleController` |
| 层 | **presentation** |
| 端口 | 由 `server.port` 决定，默认 **8102** |
| 关联 | `IdempotentChatCompletionApplicationService`、`ChatCompletionApplicationService` |

---

## 1. 背景

OpenAI 生态的工具链（SDK、CLI、第三方网关）普遍约定 **`/v1/chat/completions`** 与 **`/v1/models`** 路径与 JSON 形态。adapter 在 presentation 层提供薄控制器，将协议适配委托给 application + infrastructure，避免在 Controller 内写供应商细节。

---

## 2. 作用

1. **`POST /v1/chat/completions`**：经 `IdempotentChatCompletionApplicationService` 编排——有复合幂等键时先查 Redis 缓存；未命中则调上游、`settle` 成功后写缓存（见 [07-ChatIdempotencyResponseCache.md](./07-ChatIdempotencyResponseCache.md)）。
2. **`GET /v1/models`**：返回合并后的模型列表（由 `@Primary` 的 `FailoverRoutingAdapter.listModels()` 聚合）。

---

## 3. 触发条件

| 端点 | 条件 |
|------|------|
| Chat | `Content-Type: application/json`；body 为 JSON 对象 |
| Models | 无 body；`Accept: application/json` |

**鉴权**：本类**不**读取 `Authorization`。经网关访问时，网关已在 Pre-Route 完成 Bearer / API Key 处理，并注入 `X-User-Id` 等头（见 [00-模块总览.md](../00-模块总览.md)）。

---

## 4. 实现要点

```27:31:adapter-service/src/main/java/com/tokenhub/adapter/presentation/OpenAiCompatibleController.java
  @PostMapping(value = "/v1/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode chatCompletions(@RequestBody JsonNode body, HttpServletRequest request) {
    return idempotentChatCompletionApplicationService.complete(body, request);
  }
```

**顺序语义**（有幂等缓存时）：

- 缓存命中：直接返回，**不调上游、不调 billing**。
- 未命中：上游 `chat` → `trySettle`（2xx 才写缓存）→ 返回 body。
- `HttpServletRequest` 仅用于读取网关注入头，不用于解析 API Key。

---

## 5. 与网关路径的关系

| 客户端请求（经网关） | 网关路由 | adapter 实际路径 |
|---------------------|----------|------------------|
| `POST https://{gateway}/v1/chat/completions` | `adapter-v1` | 同路径转发 |
| `GET https://{gateway}/v1/models` | `adapter-v1` | 同路径转发 |

网关对 Chat 另有 **余额预检**（`BillingPreflightGatewayFilter`），发生在到达 adapter **之前**；adapter 不负责预检。

---

## 6. 优劣分析

| 优点 | 缺点 |
|------|------|
| Controller 极简，易测 | 未区分流式 `stream: true`（当前按非流式 JSON 处理） |
| `JsonNode` 避免过早绑定 DTO | 缺少请求体校验注解（依赖上游报错） |
| 结算与响应解耦 | 直连 adapter 时可能缺 `X-User-Id` 导致不记账 |

---

## 7. 配置项

无独立配置；行为由 `tokenhub.adapter.*`、`tokenhub.billing.*` 与注入的 Bean 决定。

---

## 8. 相关文档

- [00-模块总览.md](../00-模块总览.md)
- [components/05-BillingSettlementClient.md](./05-BillingSettlementClient.md)
- [gateway-service/docs/09-错误响应与头约定.md](../../../gateway-service/docs/09-错误响应与头约定.md)
