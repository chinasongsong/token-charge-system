# IpRiskAndQuotaGatewayFilter（O-6）

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.gateway.infrastructure.web.IpRiskAndQuotaGatewayFilter` |
| Order | `HIGHEST_PRECEDENCE + 13`（在 Idempotency +12 之后） |
| Backlog | [O-6](../../../docs/TDD/O-06-网关风控-IP与配额.md) |

---

## 1. 背景

仅有 [06-ApiKeyRedisRateLimitGatewayFilter](./06-ApiKeyRedisRateLimitGatewayFilter.md) 的**秒级 QPS** 时：

- 无法按 **客户端 IP** 封禁或强制白名单（企业客户固定出口 IP）；
- 无法限制 **单日请求次数**（与 dev-plan「日配额」对齐）。

O-6 在 **API Key 已解析**（有 `X-Api-Key-Id`）之后、秒级限流之前，增加 IP 策略与日计数。

---

## 2. 作用

**总开关** `tokenhub.gateway.risk.enabled=false`（默认）时，整个过滤器短路。

启用后，对 `/v1/**`（非 OPTIONS）：

| 步骤 | Redis 结构 | 结果 |
|------|------------|------|
| IP 黑名单 | Set `risk:ip:deny`，成员为 IP 字符串 | 命中 → `403`，`I403002` |
| IP 白名单 | Set `risk:ip:allow`；仅当 `enforce-allowlist=true` | 不在集合 → `403`，`I403003` |
| 日配额 | String 计数 `rl:daily:{apiKeyId}:{yyyyMMdd}` INCR | `count > daily-quota` → `429`，`I429002` |

**IP 解析顺序**：

1. `X-Forwarded-For` 第一个地址（逗号前）；
2. 否则 `request.getRemoteAddress()`。

**日配额跳过条件**：

- `daily-quota <= 0`（默认 0 = 不限制）；
- 无 `X-Api-Key-Id`（例如仅 JWT 且未走 Key 解析）→ 不计量。

---

## 3. 实现要点

```87:102:gateway-service/src/main/java/com/tokenhub/gateway/infrastructure/web/IpRiskAndQuotaGatewayFilter.java
  private Mono<IpDecision> checkIp(String ip) {
    if (ip == null || ip.isBlank()) {
      return Mono.just(IpDecision.OK);
    }
    Mono<Boolean> denied = redis.opsForSet().isMember("risk:ip:deny", ip).defaultIfEmpty(Boolean.FALSE);
    return denied.flatMap(d -> {
      if (Boolean.TRUE.equals(d)) {
        return Mono.just(IpDecision.DENY);
      }
      if (!enforceAllowlist) {
        return Mono.just(IpDecision.OK);
      }
      return redis.opsForSet().isMember("risk:ip:allow", ip)
          .defaultIfEmpty(Boolean.FALSE)
          .map(allowed -> Boolean.TRUE.equals(allowed) ? IpDecision.OK : IpDecision.NOT_ALLOWED);
    }).onErrorReturn(IpDecision.OK);
  }
```

Redis 异常时 IP 检查 **放行**（`onErrorReturn(OK)`）；日配额异常 **放行**（`onErrorResume`）——与缓存层类似，偏可用性。

日计数首次 INCR 时 `EXPIRE 2 days`，避免 key 泄漏。

**时区**：使用 JVM 默认时区的 `LocalDate.now()`，**未**实现 TDD 中的 `quota-time-zone` 配置项（演进项）。

**计量单位**：当前为 **请求次数**，非 token 用量；与 O-6 TDD「流式按 token 结算」有差距。

---

## 4. 运维如何写入规则

首版无 ops API，需直接操作 Redis（示例）：

```bash
# 封禁 IP
SADD risk:ip:deny 203.0.113.10

# 白名单（需 enforce-allowlist=true）
SADD risk:ip:allow 198.51.100.0

# 查看日计数
GET rl:daily:42:20260516
```

TDD 规划：`ops-console` 写 DB + 网关刷新，或 `PUT /ops/risk/ip-rules`。

---

## 5. 优劣分析

| 优点 | 缺点 |
|------|------|
| 默认关闭，不影响现网 | IP 来自 `X-Forwarded-For`，未校验可信代理链，可伪造 |
| 与 Key 维度配额解耦 | 无月配额、无按 userId 配额 |
| Set 结构简单，运营可脚本化 | 无 CIDR，仅精确 IP |
| 顺序在 Key 解析后，配额准确 | JWT-only 无 apiKeyId 时不计日配额 |

---

## 6. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **WAF / Cloudflare** | 边缘 DDoS、Bot、GeoIP |
| **Envoy RBAC + CIDR** | 数据面 IP 策略 |
| **Redis + Lua 原子脚本** | 多规则一次评估，减少往返 |
| **Sentinel / Resilience4j** | 集群限流、热点参数 |
| **按 token 计费配额** | 在 billing 结算后异步 INCR，更准确 |
| **可信代理配置** | 仅信任 `X-Forwarded-For` 当 remote 为已知 LB IP |

---

## 7. 配置项

| 配置键 | 环境变量 | 默认 |
|--------|----------|------|
| `tokenhub.gateway.risk.enabled` | `GATEWAY_RISK_ENABLED` | `false` |
| `tokenhub.gateway.risk.enforce-allowlist` | `GATEWAY_RISK_ENFORCE_ALLOWLIST` | `false` |
| `tokenhub.gateway.risk.daily-quota` | `GATEWAY_RISK_DAILY_QUOTA` | `0`（不限制） |

---

## 8. 相关文档

- [06-ApiKeyRedisRateLimitGatewayFilter.md](./06-ApiKeyRedisRateLimitGatewayFilter.md)
- [docs/TDD/O-06-网关风控-IP与配额.md](../../../docs/TDD/O-06-网关风控-IP与配额.md)
