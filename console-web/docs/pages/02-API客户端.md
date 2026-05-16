# API 客户端

控制台使用两套 HTTP 封装，对应网关后 **两种响应形态**。

---

## 1. api/client.ts（业务信封）

### 1.1 基址

```typescript
const base = import.meta.env.VITE_API_BASE ?? "";
```

开发环境 `.env.development`：`VITE_API_BASE=/api`，由 Vite 代理到网关（见 [08-构建与配置.md](../08-构建与配置.md)）。

### 1.2 鉴权头

```3:10:console-web/src/api/client.ts
function authHeader(): Record<string, string> {
  const t = localStorage.getItem("accessToken");
  const h: Record<string, string> = { "Content-Type": "application/json" };
  if (t) {
    h.Authorization = `Bearer ${t}`;
  }
  return h;
}
```

**注意**：与 Pinia `session` 使用同一 `localStorage` 键 `accessToken`；`setToken` 应同步更新两者。

### 1.3 apiJson

```12:23:console-web/src/api/client.ts
export async function apiJson<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${base}${path}`, { ...init, headers: { ...authHeader(), ...(init?.headers as object) } });
  const j = (await res.json()) as {
    code: string;
    message: string;
    data: T;
  };
  if (j.code !== "0") {
    throw new Error(j.message || `HTTP ${res.status}`);
  }
  return j.data;
}
```

| 约定 | 说明 |
|------|------|
| 成功 | `code === "0"`，返回 `data` |
| 失败 | 抛 `Error(message)`，由页面 `ElMessage` 等处理 |
| HTTP 4xx/5xx | 仍尝试解析 JSON；无 body 时用 status |

### 1.4 令牌工具

| 函数 | 作用 |
|------|------|
| `saveToken` | 写 `localStorage`（遗留，登录多用 store） |
| `clearToken` | 删除 `accessToken` |

---

## 2. api/gateway.ts（OpenAI 兼容）

用于 **`/v1/*`**：响应为 **原始 OpenAI JSON**，非 `ApiResponse` 信封。

### 2.1 Bearer 解析顺序

```22:32:console-web/src/api/gateway.ts
function resolveBearer(opts?: { preferApiKey?: boolean }): string {
  const sk = sessionStorage.getItem(EXPERIENCE_SK);
  const jwt = localStorage.getItem("accessToken");
  if (opts?.preferApiKey && sk) return sk;
  if (jwt) return jwt;
  if (sk) return sk;
  return "";
}
```

| 场景 | 行为 |
|------|------|
| 体验页 + `preferApiKey` | 优先 `sessionStorage` 中的 `sk_tokenhub_…` |
| 默认 | 优先 JWT，其次体验 SK |
| 均无 | 空 Bearer（网关返回 401） |

常量键：`tokenhub_experience_sk`。

### 2.2 fetchModels

`GET ${base}/v1/models` → 解析 `{ data: OpenAiModel[] }`。

### 2.3 chatCompletions

`POST ${base}/v1/chat/completions`，body 由 `ExperienceView` 构造（model、messages 等）。

错误：尝试解析 `{ message }` JSON，否则用响应文本。

### 2.3 体验 SK 存取

| 函数 | 存储 |
|------|------|
| `setExperienceSk` / `clearExperienceSk` | `sessionStorage` |

关闭标签页即失效，降低 SK 长期暴露面。

---

## 3. 路径与网关路由对照

| 前端 path（相对 base） | 网关下游 |
|------------------------|----------|
| `/user/*` | user-center |
| `/apikeys`、`/dashboard/*`、`/billing/*` | billing |
| `/payments/*` | payment |
| `/v1/models`、`/v1/chat/completions` | adapter（经全套 /v1 过滤器） |
| `/v1/usage` | billing |

---

## 4. 使用示例

```typescript
// 登录
const data = await apiJson<{ accessToken: string }>("/user/login", {
  method: "POST",
  body: JSON.stringify({ email, password }),
});

// 充值
await apiJson("/payments/mock/recharge", {
  method: "POST",
  body: JSON.stringify({ amount: 1000 }),
});

// 体验页聊天
await chatCompletions(payload, { preferApiKey: true });
```

---

## 5. 未使用 axios

`package.json` 含 `axios` 依赖，当前实现统一 **`fetch`**，避免双栈。新代码建议继续用 `apiJson` / `gateway.ts`。

---

## 6. 相关文档

- [03-会话状态.md](./03-会话状态.md)
- [08-构建与配置.md](../08-构建与配置.md)
- [gateway-service/docs/08-路由与配置.md](../../../gateway-service/docs/08-路由与配置.md)
