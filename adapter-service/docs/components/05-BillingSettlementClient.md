# BillingSettlementClient

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.adapter.infrastructure.billing.BillingSettlementClient` |
| 层 | **infrastructure** |
| 调用时机 | `IdempotentChatCompletionApplicationService` 在 Chat **成功返回后**（缓存命中则跳过） |
| 下游 | `POST {billingBaseUrl}/internal/billing/settle` |

---

## 1. 背景

网关已在边缘完成鉴权与（Chat 路径）余额 **预检**，但实际 token 消耗只有在上游响应返回后才可知。adapter 在拿到 OpenAI 兼容响应中的 `usage` 字段后，将用量 **异步最佳努力** 提交给 billing。扣费幂等以网关注入的 **`idempotencyKey`（复合键）** 为准；`traceId` 用于观测与订单 `trace_id` 字段（无复合键时 billing 回退 `traceId`）。

---

## 2. 作用

1. 从 **网关注入头** 读取 `X-User-Id`、`X-Api-Key-Id`、`X-Trace-Id`、`X-Idempotency-Key-Composite`、`X-Idempotency-Source`（后两者可选，Chat 经网关时应有）。
2. 从 **响应 JSON** 读取 `usage.prompt_tokens` / `usage.completion_tokens`。
3. 组装结算体并 `POST` billing 内部接口，请求头带 `X-Internal-Token`。
4. 任一步不满足或 HTTP 失败 → **静默跳过或 warn**，不影响已返回给客户端的 Chat 响应。
5. `trySettle` 返回 `boolean`：HTTP 2xx 为 `true`，供 [07-ChatIdempotencyResponseCache.md](./07-ChatIdempotencyResponseCache.md) 判定是否写入响应缓存。

---

## 3. 触发条件（全部满足才发起 HTTP）

| 条件 | 不满足时 |
|------|----------|
| `tokenhub.billing.settlement-enabled=true` | 直接 return |
| `chatResponse` 为非空对象 | return |
| `X-User-Id` 可解析为 `long` | return |
| `X-Trace-Id` 非空 | return |
| `usage` 中 input/output token 至少一项 > 0 | return |
| `X-Api-Key-Id` 若存在则必须可解析为 `long` | return（非法则放弃整单） |

**说明**：直连 adapter 且无网关头时，通常 **不会结算**——与「信任网关」模型一致。

---

## 4. 请求体与头

**URL**：`{tokenhub.billing.base-url}` + `/internal/billing/settle`  
（默认 base：`http://127.0.0.1:8103`，与网关 `TOKENS_GATEWAY_BILLING_URI` 对齐。）

**Headers**：

| 头 | 值 |
|----|-----|
| `Content-Type` | `application/json` |
| `X-Internal-Token` | `BILLING_INTERNAL_TOKEN`，默认 `dev-internal-token` |

**Body 字段**：

| 字段 | 来源 |
|------|------|
| `traceId` | `X-Trace-Id` |
| `idempotencyKey` | `X-Idempotency-Key-Composite`（可选） |
| `idempotencySource` | `X-Idempotency-Source`（可选） |
| `userId` | `X-User-Id` |
| `apiKeyId` | `X-Api-Key-Id`（可 null） |
| `providerCode` | `resolveProviderForModel(modelName)` |
| `modelName` | 请求体 `model`，空则 `deepseek-v4-flash` |
| `inputTokens` | `usage.prompt_tokens` |
| `outputTokens` | `usage.completion_tokens` |

```87:105:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/billing/BillingSettlementClient.java
    String url = base + "/internal/billing/settle";
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("traceId", traceId);
    body.put("userId", userId);
    body.put("apiKeyId", apiKeyId);
    body.put("providerCode", resolveProviderForModel(modelName));
    body.put("modelName", modelName);
    body.put("inputTokens", inputTokens);
    body.put("outputTokens", outputTokens);
    ...
    headers.set("X-Internal-Token", internalToken);
    try {
      restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
    } catch (Exception ex) {
      log.warn("billing settle failed: {}", ex.toString());
    }
```

---

## 5. providerCode 推断（故障转移场景）

故障转移到智谱时，请求/响应中的 `model` 常为 `glm-*`。若仍用默认 `tokenhub.adapter.provider-code`（`deepseek`），billing 计价可能匹配错 `model_prices`。

```108:117:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/billing/BillingSettlementClient.java
  private String resolveProviderForModel(String modelName) {
    if (modelName == null || modelName.isBlank()) {
      return providerCode;
    }
    String m = modelName.trim().toLowerCase();
    if (m.startsWith("glm") || m.contains("zhipu")) {
      return "zhipu";
    }
    return providerCode;
  }
```

---

## 6. 与网关预检的关系

| 阶段 | 服务 | 接口/逻辑 |
|------|------|-----------|
| 预检 | gateway → billing | `/internal/billing/preflight`（Chat 前） |
| 结算 | adapter → billing | `/internal/billing/settle`（Chat 后） |

预检不通过时请求到不了 adapter；预检通过但结算失败时，可能出现 **已推理未扣费**，需运营对账或补偿任务。

---

## 7. 配置项

| 配置键 | 环境变量 | 默认 |
|--------|----------|------|
| `tokenhub.billing.settlement-enabled` | `BILLING_SETTLEMENT_ENABLED` | `true` |
| `tokenhub.billing.base-url` | `TOKENS_GATEWAY_BILLING_URI` | `http://127.0.0.1:8103` |
| `BILLING_INTERNAL_TOKEN` | `BILLING_INTERNAL_TOKEN` | `dev-internal-token` |
| `tokenhub.adapter.provider-code` | — | `deepseek`（非 glm 时的默认 provider） |

---

## 8. 优劣分析

| 优点 | 缺点 |
|------|------|
| 不阻断用户响应 | 结算失败仅 warn，需监控 |
| 与网关 O-10 幂等键对齐 | 无 usage 的上游响应无法计费 |
| model 启发式区分供应商 | 新模型命名规则需维护 |

---

## 9. 相关文档

- [components/01-OpenAiCompatibleController.md](./01-OpenAiCompatibleController.md)
- [00-模块总览.md](../00-模块总览.md)
- [gateway-service/docs/filters/07-BillingPreflightGatewayFilter.md](../../../gateway-service/docs/filters/07-BillingPreflightGatewayFilter.md)
- [gateway-service/docs/filters/08-IdempotencyGatewayFilter.md](../../../gateway-service/docs/filters/08-IdempotencyGatewayFilter.md)
- `docs/TDD/O-01-扣费并发-分布式锁与悲观锁策略.md`（billing 侧幂等）
