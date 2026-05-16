# UserJwtMvcInterceptor

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.usercenter.infrastructure.web.UserJwtMvcInterceptor` |
| 注册 | `UserCenterWebConfiguration.addInterceptors` |
| 保护路径 | `/user/me`、`/user/support/**` |
| 依赖 | `JwtPrincipalResolver` → `JwtAccessTokenIssuer` |

---

## 1. 背景

用户中心同时存在**匿名接口**（注册、登录、忘记密码）与**需登录接口**（个人资料、工单）。Spring MVC 未默认启用 Spring Security 过滤器链，本仓库采用**轻量 HandlerInterceptor**：仅在明确路径上校验 JWT，避免误伤公开 API。

网关对 `/user/**` **不做** JWT 验签（仅 Trace），因此**鉴权责任在本服务**——与 `/v1/**` 在网关验签的模式不同。

---

## 2. 作用

| 场景 | 行为 |
|------|------|
| 请求路径**未**注册到拦截器 | 不执行本类（如 `POST /user/login`） |
| `Authorization` 缺失 / 非 Bearer / 验签失败 | 抛出 `BusinessException(UNAUTHORIZED, "未登录或令牌无效")` |
| Bearer JWT 合法 | 解析 `sub` 为 `long userId`，写入 `request.setAttribute(AuthConstants.REQUEST_USER_ID, uid)` |
| 后续 Controller | 从 attribute 读取 `userId`，**不再**重复解析 Header |

路径注册：

```17:20:user-center-service/src/main/java/com/tokenhub/usercenter/infrastructure/web/UserCenterWebConfiguration.java
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(userJwtMvcInterceptor).addPathPatterns("/user/me", "/user/support/**");
  }
```

---

## 3. 实现要点

### 3.1 preHandle 流程

```23:35:user-center-service/src/main/java/com/tokenhub/usercenter/infrastructure/web/UserJwtMvcInterceptor.java
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler
  ) {
    Optional<Long> uid = jwtPrincipalResolver.resolveBearer(request.getHeader("Authorization"));
    if (uid.isEmpty()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或令牌无效");
    }
    request.setAttribute(AuthConstants.REQUEST_USER_ID, uid.get());
    return true;
  }
```

### 3.2 JwtPrincipalResolver

- 使用 `ApiKeySupport.extractBearer` 剥离 `Bearer ` 前缀。
- `JwtSupport.parse(token, signingKey)` 验签；`sub` 解析为 `Long`。
- 任意异常 → `Optional.empty()`（由拦截器统一转为 401 业务异常）。

```18:28:user-center-service/src/main/java/com/tokenhub/usercenter/infrastructure/security/JwtPrincipalResolver.java
  public Optional<Long> resolveBearer(String authorizationHeader) {
    String raw = ApiKeySupport.extractBearer(authorizationHeader);
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    try {
      Claims claims = JwtSupport.parse(raw, issuer.signingKey());
      return Optional.of(Long.parseLong(claims.getSubject()));
    } catch (RuntimeException ex) {
      return Optional.empty();
    }
  }
```

### 3.3 与 Controller 的约定

Attribute 名：`com.tokenhub.usercenter.userId`（`AuthConstants.REQUEST_USER_ID`）。

示例：`UserAuthController.me`、`SupportTicketController` 各方法均依赖该 attribute，**假定**拦截器已执行。

---

## 4. 优劣分析

| 优点 | 缺点 |
|------|------|
| 路径白名单清晰，公开接口零配置 | 新增受保护路径须记得注册拦截器 |
| 与网关解耦：user API 不依赖 `X-User-Id` 注入 | 未使用 Spring Security 标准 `SecurityContext` |
| 统一 `BusinessException` → 全局异常处理 JSON | 不校验 `iss`/`aud`/scope；仅 HMAC + `sub` |
| 实现简单，易于阅读与单测 | 无方法级 `@PreAuthorize` 细粒度授权 |

---

## 5. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **Spring Security OAuth2 Resource Server** | `JwtAuthenticationConverter` + `SecurityFilterChain`，标准 claim 与角色 |
| **网关统一鉴权 + 内网信任头** | 所有 `/user/**` 在网关验 JWT 并注入 `X-User-Id`；服务只信 mTLS 内网（需防头伪造） |
| **Session + Redis** | 适合需要即时吊销、多端管理的控制台 |
| **OPA / ABAC** | 多租户、工单跨组织时策略外置 |

**建议（短期）**：保持当前拦截器；新增受保护路由时同步更新 `UserCenterWebConfiguration`。**建议（中期）**：若引入 Refresh Token 或角色（`AGENT`），迁移到 Spring Security 或显式 `AuthorizationService`。

---

## 6. 配置项

| 变量 / 配置 | 说明 |
|-------------|------|
| `JWT_SECRET` → `tokenhub.jwt.secret` | 与签发方 `JwtAccessTokenIssuer`、网关一致 |
| （无独立开关） | 拦截器始终启用 |

---

## 7. 相关文档

- [03-JwtAccessTokenIssuer.md](./03-JwtAccessTokenIssuer.md)
- [08-路由与配置.md](../08-路由与配置.md)
- [gateway filters/02-V1IngressAuthGatewayFilter.md](../../../gateway-service/docs/filters/02-V1IngressAuthGatewayFilter.md)
