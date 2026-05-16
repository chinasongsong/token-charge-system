# JwtAccessTokenIssuer

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.usercenter.infrastructure.security.JwtAccessTokenIssuer` |
| 实现端口 | `com.tokenhub.usercenter.application.port.AccessTokenIssuer` |
| 算法 | HMAC-SHA（`JwtSupport` / `common-security`） |
| 配置前缀 | `tokenhub.jwt`（`JwtProperties`） |

---

## 1. 背景

登录成功后，控制台与部分 API 调用需要**无状态 Bearer 令牌**。平台约定：

- JWT **`sub`** = 用户数字 ID（字符串形式）；
- 网关 `/v1/**` 与 billing 控制台路径使用**同一密钥**验签；
- 令牌由**用户中心签发**，其它服务只验证、不 mint（除非未来扩展服务间 token）。

---

## 2. 作用

| 方法 | 行为 |
|------|------|
| `issueForUser(long userId)` | 签发 access token，`sub = String.valueOf(userId)` |
| `accessTokenTtlSeconds()` | 返回配置的 TTL（秒），供 `TokenResponse.expiresInSeconds` |
| `signingKey()` | 供 `JwtPrincipalResolver` 验签入站请求（包内协作） |

---

## 3. 实现要点

### 3.1 启动时加载密钥

```20:24:user-center-service/src/main/java/com/tokenhub/usercenter/infrastructure/security/JwtAccessTokenIssuer.java
  @PostConstruct
  void initSigningKey() {
    String secret = Objects.requireNonNull(properties.getSecret(), "tokenhub.jwt.secret");
    this.signingKey = JwtSupport.hmacShaKeyFromUtf8(secret, 32);
  }
```

UTF-8 密钥经 `hmacShaKeyFromUtf8(..., 32)` 处理，满足 HMAC 最小长度要求（生产应使用 ≥32 字节随机 secret）。

### 3.2 签发

```26:34:user-center-service/src/main/java/com/tokenhub/usercenter/infrastructure/security/JwtAccessTokenIssuer.java
  public String issueForUser(long userId) {
    return JwtSupport.mintAccessToken(
        String.valueOf(userId),
        properties.getIssuer(),
        signingKey,
        properties.getTtlSeconds() * 1000L
    );
  }
```

| Claim / 参数 | 来源 | 典型值 |
|--------------|------|--------|
| `sub` | 参数 `userId` | `"42"` |
| `iss` | `tokenhub.jwt.issuer` | `user-center` |
| 有效期 | `ttl-seconds` × 1000 ms | 默认 86400000 ms（24h） |

### 3.3 配置绑定

`application.yml`：

```yaml
tokenhub:
  jwt:
    issuer: user-center
    secret: ${JWT_SECRET:dev-only-change-me-32bytes-minimum!!}
    ttl-seconds: 86400
```

`JwtProperties` 使用 `@ConfigurationProperties(prefix = "tokenhub.jwt")`，在 `UserCenterApplication` 上 `@EnableConfigurationProperties`。

### 3.4 与网关的关系

| 组件 | 密钥来源 | 用途 |
|------|----------|------|
| 本类 | `JWT_SECRET` | 签发 |
| `JwtPrincipalResolver` | 同上 `signingKey()` | `/user/me`、`/user/support/**` 验签 |
| `V1IngressAuthGatewayFilter` | 环境变量 `JWT_SECRET` | `/v1/**` 验签 → `X-User-Id` |

**生产必须**在各环境配置强随机 `JWT_SECRET`；开发默认值仅用于本地联调。

---

## 4. 优劣分析

| 优点 | 缺点 |
|------|------|
| 与 `common-security` 统一，减少重复 JWT 代码 | 仅 HS256 对称密钥，所有服务需保管 secret |
| 配置集中、TTL 可调 | 无 Refresh Token、无 `jti` 吊销列表 |
| `sub` 简单明了，网关直接传用户 ID | 未编码角色/权限（工单 AGENT 等未来需扩展 claim） |

---

## 5. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **RS256 非对称** | 用户中心私钥签发，网关/业务公钥验签，secret 不扩散 |
| **OAuth2 Authorization Server** | 标准 `access_token` + `refresh_token`，scope 细分 |
| **短期 AT + 长期 RT** | AT 5–15 分钟，RT 存 Redis 可吊销 |
| **Opaque token + introspection** | 网关调 `/oauth/introspect`，适合强吊销需求 |

**建议**：短期继续 HMAC + 共享 `JWT_SECRET`；用户量上来后改为 RS256 或专用 IdP；若需「踢下线」，增加 Redis 黑名单或 session 版本号 claim。

---

## 6. 配置项

| 变量 / 配置 | 默认 | 说明 |
|-------------|------|------|
| `JWT_SECRET` | （见 yml 占位） | **必填生产**；与 gateway 一致 |
| `tokenhub.jwt.issuer` | `user-center` | JWT `iss` |
| `tokenhub.jwt.ttl-seconds` | `86400` | Access token 寿命（秒） |

---

## 7. 相关文档

- [02-UserApplicationService.md](./02-UserApplicationService.md)
- [01-UserJwtMvcInterceptor.md](./01-UserJwtMvcInterceptor.md)
- [gateway filters/02-V1IngressAuthGatewayFilter.md](../../../gateway-service/docs/filters/02-V1IngressAuthGatewayFilter.md)
