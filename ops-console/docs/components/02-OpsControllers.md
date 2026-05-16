# OpsControllers

运营 HTTP API 位于 `com.tokenhub.ops.presentation`，统一返回 **`ApiResponse<T>`** 信封（`code=0` 成功）。

所有接口均需 **`X-Ops-Token`**（见 [01-OpsTokenMvcInterceptor.md](./01-OpsTokenMvcInterceptor.md)）。

---

## 1. OpsModelProviderController

| 项 | 内容 |
|----|------|
| 类 | `OpsModelProviderController` |
| 基路径 | `/ops/model-providers` |

### GET `/ops/model-providers`

查询表 `model_providers`，按 `code` 排序。

**响应 `data`**：`ModelProviderRow[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | string | 供应商标识 |
| `title` | string | 展示名 |
| `enabled` | boolean | 是否启用 |
| `baseUrl` | string | 上游 API 基址 |

```22:33:ops-console/src/main/java/com/tokenhub/ops/presentation/OpsModelProviderController.java
  @GetMapping
  public ApiResponse<List<ModelProviderRow>> list() {
    List<ModelProviderRow> rows = jdbcTemplate.query(
        "SELECT code, title, enabled, base_url FROM model_providers ORDER BY code",
        (rs, i) -> new ModelProviderRow(
            rs.getString("code"),
            rs.getString("title"),
            rs.getBoolean("enabled"),
            rs.getString("base_url")
        )
    );
    return ApiResponse.ok(rows);
  }
```

**用途**：运营核对 adapter 路由所用供应商配置；后续可扩展 PUT/PATCH 写接口（需审计与审批）。

---

## 2. OpsAuditEventController

| 项 | 内容 |
|----|------|
| 类 | `OpsAuditEventController` |
| 基路径 | `/ops/audit-events` |

### GET `/ops/audit-events`

查询参数：

| 参数 | 默认 | 约束 |
|------|------|------|
| `limit` | `100` | 钳制在 **1–500** |

SQL：`audit_events` 按 `id DESC`，返回最近事件。

**响应 `data`**：`AuditEventRow[]`

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 主键 |
| `actor` | string | 操作者标识 |
| `action` | string | 动作码 |
| `resourceType` | string | 资源类型 |
| `resourceId` | string | 资源 ID |
| `detailJson` | string | 详情 JSON 字符串 |
| `createdAt` | Instant | 创建时间（UTC） |

```33:54:ops-console/src/main/java/com/tokenhub/ops/presentation/OpsAuditEventController.java
  @GetMapping
  public ApiResponse<List<AuditEventRow>> list(
      @RequestParam(name = "limit", defaultValue = "100") int limit
  ) {
    int safe = Math.min(Math.max(limit, 1), 500);
    List<AuditEventRow> rows =
        jdbcTemplate.query(
            "SELECT id, actor, action, resource_type, resource_id, detail_json, created_at "
                + "FROM audit_events ORDER BY id DESC LIMIT ?",
            ...
        );
    return ApiResponse.ok(rows);
  }
```

**用途**：安全审计、变更追溯；写入方通常为各服务的审计埋点（非本模块职责）。

---

## 3. 错误响应

拦截器或业务异常经全局异常处理转换为 `ApiResponse`：

| 场景 | 典型 code |
|------|-----------|
| 令牌无效 | 未授权类（与 `ErrorCode.UNAUTHORIZED` 一致） |
| 数据库不可用 | 5xx / 内部错误 |

具体错误码表见仓库 `common-core` 与 [gateway-service/docs/09-错误响应与头约定.md](../../gateway-service/docs/09-错误响应与头约定.md)（若网关统一格式）。

---

## 4. 与 adapter / 数据种子

`model_providers` 行由迁移或种子脚本填充；adapter-service 运行时读取相同表（或缓存）。ops-console **只读**，修改数据需后续写 API 或 DBA 流程。

---

## 5. 相关文档

- [01-OpsTokenMvcInterceptor.md](./01-OpsTokenMvcInterceptor.md)
- [08-路由与配置.md](../08-路由与配置.md)
