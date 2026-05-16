# PaymentInternalApiGuardFilter

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.payment.infrastructure.security.PaymentInternalApiGuardFilter` |
| 类型 | `OncePerRequestFilter`，`@Order(Ordered.HIGHEST_PRECEDENCE)` |
| 头名 | `X-Internal-Token` |
| 配置 | `BILLING_INTERNAL_TOKEN`（与 billing 共用约定） |

---

## 1. 背景

`billing-service`、`payment-service` 及部分运营脚本通过 **HTTP 内部接口** 协作。这些接口不应暴露给 C 端用户，需在 Servlet 层最早拦截，与业务 JWT 分离。

业界惯例：共享密钥头（`X-Internal-Token`）、mTLS、或 Service Mesh 身份——本仓库 M1 采用共享令牌，与网关、billing 对齐。

---

## 2. 作用

1. 仅当 `requestURI` 以 `/internal/` 开头时校验令牌。
2. 请求头 `X-Internal-Token` 必须与 `BILLING_INTERNAL_TOKEN` **完全相等**。
3. 失败返回 **403**，正文 `invalid internal token`。
4. 非 `/internal/**` 路径直接放行。

---

## 3. 触发条件

| 条件 | 行为 |
|------|------|
| URI 不以 `/internal/` 开头 | 放行 |
| URI 以 `/internal/` 开头且令牌正确 | 放行 |
| URI 以 `/internal/` 开头且令牌缺失/错误 | **403** |

**注意**：`/payments/**` 用户面路径**不**经过本 Filter 的校验逻辑。

---

## 4. 实现要点

```24:41:payment-service/src/main/java/com/tokenhub/payment/infrastructure/security/PaymentInternalApiGuardFilter.java
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
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
```

常量 `HEADER = "X-Internal-Token"`。

---

## 5. 保护的 API 示例

| 方法 | 路径 |
|------|------|
| POST | `/internal/payments/recharge` |
| POST | `/internal/payments/orders/retry-credit` |
| POST | `/internal/payments/orders/{orderNo}/channel-reconcile` |
| POST | `/internal/payments/reconciliation/batches` |
| GET | `/internal/payments/reconciliation/batches/{id}` |

出站调用 billing 时，`BillingCreditClient` 同样携带此头。

---

## 6. 优劣分析

| 优点 | 缺点 |
|------|------|
| 实现简单，与 billing 一致 | 静态共享密钥泄露风险高 |
| Filter 最早执行，失败成本低 | 无 per-caller 审计身份 |
| 与 MVC 拦截器正交 | 生产应轮换密钥并配合网络隔离 |

---

## 7. 配置项

| 变量 / 配置键 | 默认 | 说明 |
|---------------|------|------|
| `BILLING_INTERNAL_TOKEN` | `dev-internal-token` | 必须与 billing、网关调 billing 时一致 |

---

## 8. 相关文档

- [02-PaymentJwtMvcInterceptor.md](./02-PaymentJwtMvcInterceptor.md)
- [08-路由与配置.md](../08-路由与配置.md)
- [00-模块总览.md](../00-模块总览.md)
