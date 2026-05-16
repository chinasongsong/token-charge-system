# OpsTokenMvcInterceptor

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.ops.infrastructure.web.OpsTokenMvcInterceptor` |
| 类型 | Spring MVC `HandlerInterceptor` |
| 注册 | `OpsWebConfiguration` → `/ops/**` |
| 请求头 | `X-Ops-Token` |

---

## 1. 背景

运营 API 的调用方是内部人员、自动化脚本或未来独立 **ops-web**，不应与用户登录态混用。采用独立于 `BILLING_INTERNAL_TOKEN` 的 **`OPS_INTERNAL_TOKEN`**，降低「一个密钥打穿所有内部面」的风险。

---

## 2. 作用

在 `preHandle` 中：

1. 读取 `X-Ops-Token`。
2. 与 `tokenhub.ops.internal-token` 配置值 **字符串相等** 比较。
3. 失败：抛出 `BusinessException(UNAUTHORIZED, "X-Ops-Token 无效")`。
4. 成功：`return true`，不写入 request 属性（当前无「运营用户 ID」模型）。

```18:28:ops-console/src/main/java/com/tokenhub/ops/infrastructure/web/OpsTokenMvcInterceptor.java
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler
  ) {
    String provided = request.getHeader("X-Ops-Token");
    if (provided == null || !provided.equals(internalToken)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "X-Ops-Token 无效");
    }
    return true;
  }
```

---

## 3. 触发条件

| 路径 | 拦截 |
|------|------|
| `/ops/**` | **是** |
| 其它（如 `/actuator/**`） | **否**（未注册拦截器） |

```17:20:ops-console/src/main/java/com/tokenhub/ops/infrastructure/web/OpsWebConfiguration.java
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(opsTokenMvcInterceptor).addPathPatterns("/ops/**");
  }
```

---

## 4. 与 payment Internal Token 对比

| 项 | ops-console | payment-service 内部 API |
|----|-------------|---------------------------|
| 头名 | `X-Ops-Token` | `X-Internal-Token` |
| 配置键 | `tokenhub.ops.internal-token` | `BILLING_INTERNAL_TOKEN` |
| 路径 | `/ops/**` | `/internal/**` |
| 实现 | MVC Interceptor | Servlet Filter |

---

## 5. 调用示例

经网关（假设已路由到 8105）：

```http
GET /ops/model-providers
X-Ops-Token: dev-ops-token
```

直连服务：

```http
GET http://127.0.0.1:8105/ops/audit-events?limit=50
X-Ops-Token: dev-ops-token
```

---

## 6. 配置项

| 配置键 | 环境变量 | 默认 |
|--------|----------|------|
| `tokenhub.ops.internal-token` | `OPS_INTERNAL_TOKEN` | `dev-ops-token` |

---

## 7. 演进建议

| 现状 | 建议 |
|------|------|
| 静态共享令牌 | 短期运维脚本可用 |
| 无操作者身份 | 审计表 `actor` 由写 API 方填入；读 API 可接 SSO JWT |
| 无 RBAC | 按资源拆令牌或接入 IAM |

---

## 8. 相关文档

- [02-OpsControllers.md](./02-OpsControllers.md)
- [08-路由与配置.md](../08-路由与配置.md)
