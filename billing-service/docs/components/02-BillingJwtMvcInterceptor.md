# BillingJwtMvcInterceptor

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.billing.infrastructure.security.BillingJwtMvcInterceptor` |
| 注册 | `BillingWebConfiguration#addInterceptors` |
| 依赖 | `BillingJwtPrincipalResolver`（`JWT_SECRET` 验签） |

---

## 1. 背景

控制台与 C 端前台经**网关**访问 `/apikeys`、`/dashboard`、`/billing/**`、`/v1/usage` 时，用户已在 `user-center-service` 登录并获得 JWT。billing 不再做登录，只验证 **Bearer JWT** 并解析 `userId` 供 Controller 使用。

与网关 `V1IngressAuthGatewayFilter` 使用同一 `JWT_SECRET` 族密钥（用户中心签发、各服务验签）。

---

## 2. 作用

1. `preHandle` 读取 `Authorization` 头。
2. `BillingJwtPrincipalResolver.resolveBearer` 解析 JWT，`sub` 为 `userId`（`Long`）。
3. 失败 → `BusinessException(UNAUTHORIZED, "未登录或令牌无效")`。
4. 成功 → `request.setAttribute(BillingAuthConstants.REQUEST_USER_ID, uid)`。

Controller 通过 `http.getAttribute(BillingAuthConstants.REQUEST_USER_ID)` 取当前用户，**不信任**请求体中的 `userId`（内部 API 除外）。

---

## 3. 触发条件（路径名单）

由 `BillingWebConfiguration` 注册：

| 模式 | 说明 |
|------|------|
| `/apikeys`、`/apikeys/**` | API Key 管理 |
| `/dashboard/**` | 仪表盘摘要 |
| `/billing/**` | 充值、订阅、订单、发票、退款等 |
| `/v1/usage` | 用量查询（OpenAI 风格路径，经网关转到 billing） |
| **排除** `/internal/**` | 走 `InternalApiGuardFilter` |

**未列入**的路径（若存在）不经过本拦截器。

---

## 4. 实现要点

**拦截器：**

```22:34:billing-service/src/main/java/com/tokenhub/billing/infrastructure/security/BillingJwtMvcInterceptor.java
  public boolean preHandle(...) {
    Optional<Long> uid = resolver.resolveBearer(request.getHeader("Authorization"));
    if (uid.isEmpty()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或令牌无效");
    }
    request.setAttribute(BillingAuthConstants.REQUEST_USER_ID, uid.get());
    return true;
  }
```

**JWT 解析（`BillingJwtPrincipalResolver`）：**

- 配置：`${JWT_SECRET:dev-only-change-me-32bytes-minimum!!}`
- `ApiKeySupport.extractBearer` 剥离 `Bearer ` 前缀
- `JwtSupport.parse` + `claims.getSubject()` → `Long userId`

---

## 5. 与网关鉴权的区别

| 维度 | 网关 `/v1/**` | billing 控制台路径 |
|------|---------------|-------------------|
| Bearer 形态 | JWT **或** `sk_*` API Key | 仅 JWT（API Key 不走本拦截器） |
| 结果 | 注入 `X-User-Id` 等头转发 adapter | 写入 `request` attribute |
| 内部结算 | 不适用 | 不适用（走 `/internal/**`） |

---

## 6. 优劣分析

| 优点 | 缺点 |
|------|------|
| 与 Spring MVC 生态一致，Controller 代码简洁 | 路径名单需手动维护，新增公开 API 易漏注册 |
| 与用户中心 JWT 模型统一 | 未校验角色/权限（仅身份） |
| 明确排除 `/internal/**` | 直连 billing 端口绕过网关时仍须 HTTPS + 网络策略 |

---

## 7. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **Spring Security Resource Server** | 标准 OAuth2/JWT 过滤器链、scope/role |
| **网关统一鉴权 + 内网信任头** | billing 只信 `X-User-Id`（需 mTLS） |
| **OPA / 外部授权** | 细粒度策略 |

---

## 8. 配置项

| 配置 / 环境变量 | 默认 | 说明 |
|-----------------|------|------|
| `JWT_SECRET` | `dev-only-change-me-32bytes-minimum!!` | 至少 32 字节；与用户中心、网关一致 |

---

## 9. 相关文档

- [01-InternalApiGuardFilter.md](./01-InternalApiGuardFilter.md)
- [03-ApiKeyApplicationService.md](./03-ApiKeyApplicationService.md)
- [08-路由与配置.md](../08-路由与配置.md)
- [gateway filters/02-V1IngressAuthGatewayFilter.md](../../gateway-service/docs/filters/02-V1IngressAuthGatewayFilter.md)
