# ApiKeyResolutionCache（O-5）

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.gateway.infrastructure.cache.ApiKeyResolutionCache` |
| 调用方 | `BillingApiKeyResolveGatewayFilter` |
| Backlog | [O-5](../../../docs/TDD/O-05-Redis缓存-APIKey解析与余额.md) |

> 说明：这不是 `GlobalFilter`，而是被解析过滤器调用的 **Redis 缓存组件**；阅读网关时建议与 [03-BillingApiKeyResolveGatewayFilter.md](./03-BillingApiKeyResolveGatewayFilter.md) 一起看。

---

## 1. 背景

`BillingApiKeyResolveGatewayFilter` 在缓存未命中时，每个请求都会对 billing 发起一次 HTTP。高并发下：

- billing CPU / 连接数上升；
- 网关额外 RTT；
- 恶意随机 Key 可能导致 **缓存击穿**（大量 miss 打 DB）。

O-5 在网关侧增加 **短 TTL 正/负缓存**，降低平均延迟并保护 billing。

---

## 2. 作用

| 操作 | 行为 |
|------|------|
| `get(fingerprint)` | 未启用 → `Mono.empty()`（等同 miss）；命中 JSON → `Entry.present`；命中 `NEG` → `Entry.notFound()` |
| `putPresent` | 写入 `userId`、`apiKeyId`，TTL = `positive-ttl-seconds` |
| `putNegative` | 写入字面量 `NEG`，TTL = `negative-ttl-seconds`，抑制无效 Key |

Redis Key 格式：

```
cache:gw:apikey:fp:{sha256Hex}
```

---

## 3. 实现要点

```64:66:gateway-service/src/main/java/com/tokenhub/gateway/infrastructure/cache/ApiKeyResolutionCache.java
  private static String keyOf(String fingerprint) {
    return "cache:gw:apikey:fp:" + fingerprint;
  }
```

**正向值**：Jackson 序列化的 `Entry` JSON（`negative=false`）。

**负向值**：固定字符串 `NEG`（非 JSON），避免与正向混淆。

**错误降级**：Redis 读/写异常 → 打 warn 日志，读返回 empty（回源 billing），写忽略——**宁可多打 billing 也不误拒合法用户**。

**失效策略**（当前）：

- 仅依赖 TTL；Key 在 billing 禁用后，最长 `positive-ttl-seconds`（默认 60s）内网关仍可能认为有效。
- TDD 规划：billing disable 时 Redis DEL 或 PUB/SUB 主动驱逐（M2）。

---

## 4. 与过滤器的协作

```71:78:gateway-service/src/main/java/com/tokenhub/gateway/infrastructure/web/BillingApiKeyResolveGatewayFilter.java
    return resolutionCache.get(fingerprint)
        .flatMap(entry -> {
          if (entry.negative()) {
            return rejectUnauthorized(exchange);
          }
          return forwardWithResolution(exchange, chain, request, entry.userId(), entry.apiKeyId());
        })
        .switchIfEmpty(Mono.defer(() -> resolveFromBilling(exchange, chain, request, fingerprint)));
```

billing 404/错误 → `putNegative` → 401。

---

## 5. 优劣分析

| 优点 | 缺点 |
|------|------|
| 实现简单，开关默认关，渐进启用 | 最终一致；禁用 Key 非即时 |
| 负缓存防击穿 | 无余额缓存（O-5 TDD 余额部分尚未在本类实现） |
| 与过滤器内聚，无额外 Order | 多网关实例共享 Redis，需统一 TTL 策略 |

---

## 6. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **Caffeine L1 + Redis L2** | 进程内纳秒级命中，减少 Redis QPS |
| **版本戳 / etag** | 缓存值带 `keyVersion`，billing 禁用时递增版本 |
| **Pub/Sub 失效** | `billing` 发 `apikey:disabled:{id}`，网关订阅 DEL |
| **只读副本 + 短查询** | 缓存 miss 读从库而非 HTTP（需把解析逻辑下沉到共享库） |

---

## 7. 配置项

| 配置键 | 环境变量 | 默认 |
|--------|----------|------|
| `tokenhub.gateway.apikey-cache.enabled` | `GATEWAY_APIKEY_CACHE_ENABLED` | `false` |
| `tokenhub.gateway.apikey-cache.positive-ttl-seconds` | `GATEWAY_APIKEY_CACHE_POS_TTL` | `60` |
| `tokenhub.gateway.apikey-cache.negative-ttl-seconds` | `GATEWAY_APIKEY_CACHE_NEG_TTL` | `10` |

---

## 8. 相关文档

- [03-BillingApiKeyResolveGatewayFilter.md](./03-BillingApiKeyResolveGatewayFilter.md)
- [docs/architecture/技术负债与路线图.md](../../../docs/architecture/技术负债与路线图.md)（O-5 状态）
