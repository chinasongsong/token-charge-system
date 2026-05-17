# ApiKeyRedisRateLimitGatewayFilter

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.gateway.infrastructure.web.ApiKeyRedisRateLimitGatewayFilter` |
| Order | `HIGHEST_PRECEDENCE + 14` |

---

## 1. 背景

模型 API 以 **POST** 为主，单 Key 可能在短时间内发起大量 Chat 请求，需保护：

- 平台与下游供应商不被打爆；
- 单用户公平性；
- billing / adapter 资源。

在网关用 **Redis 固定窗口计数** 实现秒级 QPS，成本低、与现有 Redis 依赖一致。

---

## 2. 作用

| 条件 | 行为 |
|------|------|
| 非 `POST` | 放行 |
| 非 `/v1/**` | 放行 |
| 无 `X-Api-Key-Id` 且无 `X-User-Id` | 放行（无法分桶） |
| 有 `X-Api-Key-Id` | 桶后缀 `ak:{id}` |
| 仅有 `X-User-Id` | 桶后缀 `uid:{id}` |
| 当前秒计数 > `rate-limit-per-second` | `429`，`I429001`，「请求过于频繁」 |

Redis Key：

```
rl:{ak:42|uid:7}:s:{epochSecond}
```

首次计数为 1 时 `EXPIRE 3s`（窗口略大于 1 秒，避免边界漂移）。

---

## 3. 实现要点

```56:78:gateway-service/src/main/java/com/tokenhub/gateway/infrastructure/web/ApiKeyRedisRateLimitGatewayFilter.java
    long sec = Instant.now().getEpochSecond();
    String key = "rl:" + suffix + ":s:" + sec;

    return redis.opsForValue()
        .increment(key)
        .flatMap(count -> {
          if (count != null && count == 1L) {
            return redis.expire(key, Duration.ofSeconds(3)).thenReturn(count);
          }
          return Mono.just(count);
        })
        .flatMap(count -> {
          if (count != null && count > perSecond) {
            ...
          }
          return chain.filter(exchange);
        });
```

**算法本质**：**固定窗口**（每秒一个 key），非滑动窗口、非令牌桶。

**与 O-6 关系**：日配额在本过滤器**之前**；秒级超限返回 `I429001`，日配额耗尽返回 `I429002`。

---

## 4. 优劣分析

| 优点 | 缺点 |
|------|------|
| 实现极简，易理解 | 固定窗口边界可能瞬间 2× 峰值（窗口交界） |
| 按 Key 优先、JWT 用户兜底 | 多实例依赖 Redis 时钟一致（秒级） |
| 仅限制 POST /v1，减少误伤 | GET 列表类接口不限速 |
| Redis INCR 原子 | Redis 故障时当前代码**未**降级，可能 5xx（与 O-6 不同） |

---

## 5. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **滑动窗口 / 令牌桶** | Redis Lua + ZSET 或 Guava `RateLimiter`；更平滑 |
| **Spring Cloud Gateway RequestRateLimiter** | 内置 `RedisRateLimiter` + `KeyResolver` |
| **Sentinel 热点参数** | 控制台动态规则 |
| **分布式协调限流** | 按模型、按租户多维度 |
| **429 + Retry-After** | 当前未返回 `Retry-After` 头，客户端退避不友好 |

**建议演进**：

1. 使用 Gateway 官方 `RequestRateLimiter` 过滤器配置化；
2. Redis 异常时 `onErrorResume` 放行或本地降级；
3. 响应头增加 `X-RateLimit-Remaining`。

---

## 6. 配置项

| 配置键 | 环境变量 | 默认 |
|--------|----------|------|
| `tokenhub.gateway.rate-limit-per-second` | `GATEWAY_RATE_LIMIT_PER_SECOND` | `60` |

---

## 7. 相关文档

- [05-IpRiskAndQuotaGatewayFilter.md](./05-IpRiskAndQuotaGatewayFilter.md)
- [07-BillingPreflightGatewayFilter.md](./07-BillingPreflightGatewayFilter.md)
