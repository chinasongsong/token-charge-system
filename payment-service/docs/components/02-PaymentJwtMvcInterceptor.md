# PaymentJwtMvcInterceptor

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.payment.infrastructure.security.PaymentJwtMvcInterceptor` |
| 类型 | Spring MVC `HandlerInterceptor` |
| 注册 | `PaymentWebConfiguration` |
| 依赖 | `PaymentJwtPrincipalResolver` |
| 写入属性 | `PaymentAuthConstants.REQUEST_USER_ID`（`tokenhubUserId`） |

---

## 1. 背景

C 端控制台经网关访问 `POST /payments/mock/recharge` 等接口时，携带与用户中心签发的相同 **JWT**（`Authorization: Bearer`）。支付服务需在 Controller 之前解析出 `userId`，避免客户端在 body 中伪造用户 ID。

---

## 2. 作用

1. 从 `Authorization` 解析 Bearer。
2. 调用 `PaymentJwtPrincipalResolver.resolveBearer` 验签并取 `sub` 作为 `Long userId`。
3. 失败抛出 `BusinessException(UNAUTHORIZED, "未登录或令牌无效")`。
4. 成功：`request.setAttribute("tokenhubUserId", userId)`。

---

## 3. 触发条件

由 `PaymentWebConfiguration` 注册：

| 规则 | 值 |
|------|-----|
| `addPathPatterns` | `/payments/**` |
| `excludePathPatterns` | `/payments/mock/callback` |

| 路径 | JWT 拦截 |
|------|----------|
| `POST /payments/mock/recharge` | **是** |
| `POST /payments/mock/checkout` | **是** |
| `POST /payments/mock/callback` | **否**（走 HMAC） |
| `/internal/**` | **否**（走 Internal Token Filter） |

---

## 4. 实现要点

```22:34:payment-service/src/main/java/com/tokenhub/payment/infrastructure/security/PaymentJwtMvcInterceptor.java
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler
  ) {
    Optional<Long> uid = resolver.resolveBearer(request.getHeader("Authorization"));
    if (uid.isEmpty()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或令牌无效");
    }
    request.setAttribute(PaymentAuthConstants.REQUEST_USER_ID, uid.get());
    return true;
  }
```

`PaymentJwtPrincipalResolver` 使用 `JWT_SECRET`（UTF-8，至少 32 字节）构建 HMAC 密钥，与 `user-center-service` 签发算法一致（`JwtSupport`）。

Controller 侧读取示例：

```31:32:payment-service/src/main/java/com/tokenhub/payment/presentation/MockPaymentController.java
    Long userId = (Long) http.getAttribute(PaymentAuthConstants.REQUEST_USER_ID);
    return ApiResponse.ok(mockPaymentApplicationService.mockRecharge(userId, request.amount()));
```

---

## 5. 与网关的关系

网关对 `/payments/**` 通常**仅做 Trace**，不在边缘验 JWT。JWT 在 **payment-service 进程内** 校验，与 billing 控制台 API 模式一致。

生产环境：`JWT_SECRET` 必须与 user-center 相同，且足够强度。

---

## 6. 配置项

| 变量 | 默认 | 说明 |
|------|------|------|
| `JWT_SECRET` | `dev-only-change-me-32bytes-minimum!!` | 与 user-center 一致 |

---

## 7. 相关文档

- [01-PaymentInternalApiGuardFilter.md](./01-PaymentInternalApiGuardFilter.md)
- [03-PaymentExecutionService.md](./03-PaymentExecutionService.md)
- [08-路由与配置.md](../08-路由与配置.md)
