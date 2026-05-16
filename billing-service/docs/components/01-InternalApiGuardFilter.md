# InternalApiGuardFilter

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.billing.infrastructure.security.InternalApiGuardFilter` |
| Order | `Ordered.HIGHEST_PRECEDENCE`（Servlet Filter 链最前） |
| 关联 | 所有 `/internal/**`；与网关 `BILLING_INTERNAL_TOKEN` 对齐 |

---

## 1. 背景

`billing-service` 同时暴露**控制台 JWT API** 与**服务间内部 API**。内部接口不能依赖用户 JWT（网关、adapter 以平台身份调用），但也不能对公网裸奔。

业界惯例：内网 + 共享密钥头（或 mTLS / Service Mesh 身份）。本仓库 M1 采用 **`X-Internal-Token`** 与配置项 `BILLING_INTERNAL_TOKEN` 对齐（与 [gateway-service 配置](../../gateway-service/docs/08-路由与配置.md) 中 `tokenhub.gateway.internal-token` 一致）。

---

## 2. 作用

1. 仅当 `requestURI` 以 `/internal/` 开头时校验。
2. 读取请求头 `X-Internal-Token`，与 `BILLING_INTERNAL_TOKEN` **全等**比较。
3. 不匹配 → `403 Forbidden`，body 为 Servlet 默认错误文案 `invalid internal token`。
4. 非内部路径 → 直接放行（JWT 由 MVC 拦截器另行处理）。

---

## 3. 触发条件

| 条件 | 行为 |
|------|------|
| URI **不**以 `/internal/` 开头 | 放行 |
| URI 以 `/internal/` 开头且 Token 正确 | 放行 |
| URI 以 `/internal/` 开头且 Token 缺失/错误 | `403` |

---

## 4. 实现要点

```15:41:billing-service/src/main/java/com/tokenhub/billing/infrastructure/security/InternalApiGuardFilter.java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalApiGuardFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Internal-Token";

  @Value("${BILLING_INTERNAL_TOKEN:dev-internal-token}")
  private String expectedToken;

  @Override
  protected void doFilterInternal(...) {
    String uri = request.getRequestURI();
    if (uri == null || !uri.startsWith("/internal/")) {
      filterChain.doFilter(request, response);
      return;
    }
    String presented = request.getHeader(HEADER);
    if (presented == null || !presented.equals(expectedToken)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "invalid internal token");
      return;
    }
    filterChain.doFilter(request, response);
  }
}
```

受保护的典型端点见 [08-路由与配置.md](../08-路由与配置.md)「内部 API」表。

---

## 5. 与 JWT 拦截器的关系

`BillingWebConfiguration` 显式 **exclude** `/internal/**`，避免控制台 JWT 与内部 Token 双重校验冲突：

```18:21:billing-service/src/main/java/com/tokenhub/billing/infrastructure/security/BillingWebConfiguration.java
    registry.addInterceptor(jwtInterceptor)
        .addPathPatterns("/apikeys", "/apikeys/**", "/dashboard/**", "/billing/**", "/v1/usage")
        .excludePathPatterns("/internal/**");
```

---

## 6. 优劣分析

| 优点 | 缺点 |
|------|------|
| 实现简单，与网关 WebClient 配置一致 | 静态共享密钥，轮换需全链路同步 |
| 过滤器最早执行，失败成本低 | 未绑定调用方身份（谁持有 Token 谁都能调） |
| 与路径前缀绑定，误配面小 | 生产应配合网络隔离，不能仅靠 Token |

---

## 7. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **mTLS** | 证书双向认证，无共享密码 |
| **OAuth2 Client Credentials** | 服务主体令牌、可审计轮换 |
| **Service Mesh（SPIFFE/SPIRE）** | 工作负载身份自动注入 |
| **IP 白名单 + Token** | 纵深防御 |

---

## 8. 配置项

| 配置 / 环境变量 | 默认 | 说明 |
|-----------------|------|------|
| `BILLING_INTERNAL_TOKEN` | `dev-internal-token` | 与网关 `internal-token` 一致 |

`application.yml` 中直接写默认值；生产用环境变量覆盖（勿在 YAML 写 `${BILLING_INTERNAL_TOKEN:...}` 自引用，见文件注释）。

---

## 9. 相关文档

- [02-BillingJwtMvcInterceptor.md](./02-BillingJwtMvcInterceptor.md)
- [08-路由与配置.md](../08-路由与配置.md)
- [gateway filters/03-BillingApiKeyResolveGatewayFilter.md](../../gateway-service/docs/filters/03-BillingApiKeyResolveGatewayFilter.md)
- [00-模块总览.md](../00-模块总览.md)
