# V1IngressAuthGatewayFilter

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.gateway.infrastructure.web.V1IngressAuthGatewayFilter` |
| Order | `HIGHEST_PRECEDENCE + 10` |
| 依赖 | `common-security`：`ApiKeySupport`、`JwtSupport` |

---

## 1. 背景

OpenAI 兼容 API（`/v1/**`）面向**开发者调用**，认证方式为 `Authorization: Bearer`。

平台同时支持两类 Bearer：

1. **控制台 JWT**（登录后短期令牌，三段式 `header.payload.sig`）。
2. **长期 API Key**（`sk_...`，非 JWT 形态）。

若不在网关区分，下游需重复解析；若在网关一刀切验 JWT，API Key 会被误判。因此本过滤器只做「**入口形态校验 + JWT 轨**」，API Key 交给下一个过滤器。

---

## 2. 作用

| 场景 | 行为 |
|------|------|
| 非 `/v1/**` | 直接放行 |
| `OPTIONS` / 非 GET·POST·PATCH | 直接放行（CORS 与其它动词） |
| `/v1/**` 无 Bearer | `401`，`I401001`，「缺少 Authorization Bearer」 |
| Bearer **像 JWT**（恰好 2 个 `.`）且配置了 `JWT_SECRET` | 验签，取 `sub` → 请求头 `X-User-Id` |
| Bearer 像 JWT 但验签失败 | `401`，「未登录或令牌无效」 |
| Bearer 像 JWT 但 **未配置** `JWT_SECRET` | **放行**（开发模式；生产必须配置） |
| Bearer **不像 JWT** | 放行，由 `BillingApiKeyResolveGatewayFilter` 处理 |

---

## 3. JWT 启发式

不解析 payload，仅统计 `.` 个数是否为 2：

```82:89:gateway-service/src/main/java/com/tokenhub/gateway/infrastructure/web/V1IngressAuthGatewayFilter.java
  private static boolean looksLikeJwt(String bearer) {
    int dots = 0;
    for (int i = 0; i < bearer.length(); i++) {
      if (bearer.charAt(i) == '.') {
        dots++;
      }
    }
    return dots == 2;
  }
```

**局限**：极少数 API Key 若含两个 `.` 会被误判为 JWT；平台 Key 通常为 `sk_` 前缀无点号。

---

## 4. 实现要点

- 密钥：`JWT_SECRET` 环境变量，UTF-8 字符串，经 `JwtSupport.hmacShaKeyFromUtf8(..., 32)` 转为 HMAC 密钥（至少 32 字节有效长度要求以实现为准）。
- 注入头：`X-User-Id` = JWT `sub`（与 `GatewayIngressHeaders.USER_ID` 同名，本类内写死常量）。

```62:76:gateway-service/src/main/java/com/tokenhub/gateway/infrastructure/web/V1IngressAuthGatewayFilter.java
    if (looksLikeJwt(bearer)) {
      if (jwtSigningKey == null) {
        return chain.filter(exchange);
      }
      try {
        Claims claims = JwtSupport.parse(bearer, jwtSigningKey);
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
          return unauthorized(exchange, "令牌无效");
        }
        ServerHttpRequest mutated = exchange.getRequest().mutate().header(USER_ID_HEADER, sub).build();
        return chain.filter(exchange.mutate().request(mutated).build());
      } catch (RuntimeException ex) {
        return unauthorized(exchange, "未登录或令牌无效");
      }
    }
```

---

## 5. 优劣分析

| 优点 | 缺点 |
|------|------|
| 与 OpenAI SDK 习惯一致（Bearer） | JWT/API Key 分流靠启发式，非 Content-Based |
| JWT 在边缘验签，减轻 adapter 负担 | 无 `JWT_SECRET` 时 JWT 不验签，存在误配风险 |
| 错误体系统一（`GatewayJsonResponses`） | 未处理 Key 轮换、scope、过期（在 billing / user-center） |

---

## 6. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **OAuth2 Resource Server** | Spring Security OAuth2 在 Gateway 用 `ReactiveJwtDecoder`，标准 claim 校验、`aud`/`exp` |
| **双 Header** | `Authorization: Bearer` 仅 JWT，`X-Api-Key` 专用于 Key，避免启发式（OpenAI 仅用 Bearer，故本平台合并） |
| **API Gateway 插件** | Kong `jwt` / `key-auth` 分路由或分 consumer |
| **mTLS + 短令牌** | B2B 场景证书身份 + 极短 JWT |

**建议**：生产强制 `JWT_SECRET`；启动时若 `/v1` 路由存在且 secret 为空则 **fail-fast**；长期可考虑 `sk-` 前缀显式分支而非数点号。

---

## 7. 配置项

| 变量 / 配置 | 说明 |
|-------------|------|
| `JWT_SECRET` | HMAC 密钥；空则跳过 JWT 验签 |

---

## 8. 相关文档

- [03-BillingApiKeyResolveGatewayFilter.md](./03-BillingApiKeyResolveGatewayFilter.md)
- [09-错误响应与头约定.md](../09-错误响应与头约定.md)
