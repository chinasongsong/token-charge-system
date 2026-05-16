# OpenAiCompatibleController

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.adapter.presentation.OpenAiCompatibleController` |
| 层 | **presentation** |
| 端口 | 由 `server.port` 决定，默认 **8102** |
| 关联 | `ChatCompletionApplicationService`、`BillingSettlementClient` |

---

## 1. 背景

OpenAI 生态的工具链（SDK、CLI、第三方网关）普遍约定 **`/v1/chat/completions`** 与 **`/v1/models`** 路径与 JSON 形态。adapter 在 presentation 层提供薄控制器，将协议适配委托给 application + infrastructure，避免在 Controller 内写供应商细节。

---

## 2. 作用

1. **`POST /v1/chat/completions`**：接收 OpenAI 风格请求体（`JsonNode`），调用应用服务转发上游，返回上游 JSON；响应返回前触发 **最佳努力** 计费结算。
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

```27:37:adapter-service/src/main/java/com/tokenhub/adapter/presentation/OpenAiCompatibleController.java
  @PostMapping(value = "/v1/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode chatCompletions(@RequestBody JsonNode body, HttpServletRequest request) {
    JsonNode response = chatCompletionApplicationService.chat(body);
    billingSettlementClient.trySettle(request, body, response);
    return response;
  }

  @GetMapping(value = "/v1/models", produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode listModels() {
    return chatCompletionApplicationService.models();
  }
```

**顺序语义**：

- 先完成上游调用并拿到 `response`，再 `trySettle`——结算失败**不会**改变已返回给客户端的 HTTP 200 与 body（除非上游本身报错）。
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
