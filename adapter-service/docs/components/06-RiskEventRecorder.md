# RiskEventRecorder

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.adapter.infrastructure.risk.RiskEventRecorder` |
| 层 | **infrastructure** |
| 存储 | MySQL 表 `risk_events`（MyBatis-Plus `RiskEventMapper`） |
| 主要调用方 | `FailoverRoutingAdapter`（供应商故障转移） |

---

## 1. 背景

自动故障转移能提高可用性，但也会掩盖上游劣化（例如 DeepSeek 持续 5xx 却长期走智谱）。将每次 **provider 级 failover** 写入审计表，便于运营后台、告警与事后分析，而不依赖分散的应用日志。

---

## 2. 作用

提供 `recordProviderFailover(fromProvider, toProvider, detail)`：

- 写入事件类型 **`provider_failover`**
- 严重级别 **`WARN`**
- 上下文 JSON：`from`、`to`、`detail`（根因摘要）

插入失败时 **仅 warn**，不影响 Chat 响应（与结算客户端策略一致）。

---

## 3. 触发条件

仅在 `FailoverRoutingAdapter` 判定 **可恢复失败** 且即将调用 **secondary** 之前调用：

```81:86:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/routing/FailoverRoutingAdapter.java
      riskEventRecorder.recordProviderFailover(
          nameOf(primary),
          nameOf(secondary),
          rootCauseMessage(ex)
      );
```

典型 `from` / `to`：

| 场景 | from | to |
|------|------|-----|
| DeepSeek 失败 → 智谱 | `deepseek` | `zhipu` |
| 智谱失败 → DeepSeek | `zhipu` | `deepseek` |

`detail` 为异常链最底层 `message`，或异常类名。

---

## 4. 实现要点

```24:37:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/risk/RiskEventRecorder.java
  public void recordProviderFailover(String fromProvider, String toProvider, String detail) {
    try {
      RiskEventPo row = new RiskEventPo();
      row.setEventType("provider_failover");
      row.setSeverity("WARN");
      ObjectNode ctx = objectMapper.createObjectNode();
      ctx.put("from", fromProvider);
      ctx.put("to", toProvider);
      ctx.put("detail", detail == null ? "" : detail);
      row.setContextJson(objectMapper.writeValueAsString(ctx));
      riskEventMapper.insert(row);
    } catch (Exception ex) {
      log.warn("risk_events insert failed: {}", ex.toString());
    }
  }
```

**表结构（PO 字段）**：

| 列 | 说明 |
|----|------|
| `event_type` | 固定 `provider_failover` |
| `severity` | 固定 `WARN` |
| `context_json` | JSON 字符串 |
| `user_id` | 当前实现**未**写入（可为 null） |
| `created_at` | 由 DB 默认或 MyBatis 填充 |

后续若要在运营台按用户筛选 failover，可在 Controller 层传入 `X-User-Id` 并扩展本方法。

---

## 5. context_json 示例

```json
{
  "from": "deepseek",
  "to": "zhipu",
  "detail": "CircuitBreaker 'deepseek' is OPEN and does not permit further calls"
}
```

---

## 6. 配置项

无独立开关；依赖 **MySQL 数据源**（`spring.datasource.*`）。DB 不可用时插入失败，仅日志告警。

---

## 7. 与网关风控的区别

| 维度 | 网关 `IpRiskAndQuotaGatewayFilter` | adapter `RiskEventRecorder` |
|------|-----------------------------------|----------------------------|
| 时机 | 请求进入 adapter **前** | 上游首跳失败、切换对端时 |
| 存储 | Redis（名单/配额） | MySQL `risk_events` |
| 事件 | IP、日配额等 | `provider_failover` |

二者互补，不重复。

---

## 8. 相关文档

- [02-FailoverRoutingAdapter.md](./02-FailoverRoutingAdapter.md)
- [00-模块总览.md](../00-模块总览.md)
- [gateway-service/docs/filters/05-IpRiskAndQuotaGatewayFilter.md](../../../gateway-service/docs/filters/05-IpRiskAndQuotaGatewayFilter.md)
