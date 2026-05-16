# WeightedRoutingPolicy

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.adapter.infrastructure.routing.WeightedRoutingPolicy` |
| 接口 | `com.tokenhub.adapter.domain.routing.RoutingPolicy` |
| 层 | **infrastructure**（实现 domain 策略接口） |
| 关联 | `ProviderRoute`、`ZhipuProviderAdapter.isConfigured()` |

---

## 1. 背景

多供应商并存时，若始终固定主备顺序，无法做 **流量比例实验**（例如 30% 走智谱以压测或比价）。`WeightedRoutingPolicy` 在 **每次 Chat 请求** 上按整数权重做一次随机首跳，失败后的 failover 仍由 [02-FailoverRoutingAdapter.md](./02-FailoverRoutingAdapter.md) 负责。

---

## 2. 作用

根据配置的 `weight-deepseek` 与 `weight-zhipu`，在 `[0, total)` 上均匀随机，决定首跳是 `DEEPSEEK` 还是 `ZHIPU`。

默认配置：**DeepSeek 70、智谱 30**（与 `application.yml` 一致）。

---

## 3. 触发条件

每次 `FailoverRoutingAdapter.chat()` 调用 `chooseFirstHop()` 时执行。

**短路规则**（不掷骰子）：

| 条件 | 结果 |
|------|------|
| 智谱未配置 API Key（`!zhipuProviderAdapter.isConfigured()`） | 恒 `DEEPSEEK` |
| `weight-zhipu <= 0` | 恒 `DEEPSEEK` |
| `weight-deepseek <= 0` 且智谱可用 | 恒 `ZHIPU` |

权重在构造时被 `Math.max(0, w)` 钳制，避免负值。

---

## 4. 实现要点

```30:41:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/routing/WeightedRoutingPolicy.java
  public ProviderRoute chooseFirstHop() {
    if (!zhipuProviderAdapter.isConfigured() || zhipuWeight <= 0) {
      return ProviderRoute.DEEPSEEK;
    }
    if (deepseekWeight <= 0) {
      return ProviderRoute.ZHIPU;
    }
    int total = deepseekWeight + zhipuWeight;
    int r = ThreadLocalRandom.current().nextInt(total);
    return r < deepseekWeight ? ProviderRoute.DEEPSEEK : ProviderRoute.ZHIPU;
  }
```

**概率**：在智谱可用且两权重均正时，DeepSeek 首跳概率为 `deepseekWeight / (deepseekWeight + zhipuWeight)`。

**与 Failover 的分工**：

- `RoutingPolicy`：只决定 **first hop**
- `FailoverRoutingAdapter`：first hop 失败后的 **second hop** 与熔断

---

## 5. 示例（默认 70/30）

| 随机数 `r`（`total=100`） | 首跳 |
|---------------------------|------|
| `0 … 69` | DeepSeek |
| `70 … 99` | 智谱（需 `ZHIPU_API_KEY`） |

若 DeepSeek 首跳失败且可恢复 → 可能 failover 到智谱；若首跳已是智谱且失败 → 可能 failover 到 DeepSeek。

---

## 6. 配置项

| 配置键 | 环境变量 | 默认 |
|--------|----------|------|
| `tokenhub.adapter.routing.weight-deepseek` | `ROUTING_WEIGHT_DEEPSEEK` | `70` |
| `tokenhub.adapter.routing.weight-zhipu` | `ROUTING_WEIGHT_ZHIPU` | `30` |

权重为**相对整数**，不必相加为 100；例如 `7` + `3` 与 `70` + `30` 等价。

---

## 7. 优劣分析

| 优点 | 缺点 |
|------|------|
| 实现简单、无状态 | 无会话粘性，同一用户连续请求可能落不同供应商 |
| 易通过环境变量调比例 | 未考虑单价、延迟、配额等业务权重 |
| 智谱未配置时自动退化单供应商 | 与 DB `model_providers` 启用状态无联动 |

---

## 8. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **加权轮询 / 平滑 WRR** | 长期比例更稳定，减少短期抖动 |
| **基于延迟/错误率的动态权重** | 配合熔断指标自动调流 |
| **按 model 路由表** | 用户指定 `glm-*` 时直连智谱，避免 failover 改 model |
| **Service Mesh 流量分裂** | 基础设施层 AB，应用无感 |

---

## 9. 相关文档

- [02-FailoverRoutingAdapter.md](./02-FailoverRoutingAdapter.md)
- [04-ProviderAdapters.md](./04-ProviderAdapters.md)
- [08-路由与配置.md](../08-路由与配置.md)
