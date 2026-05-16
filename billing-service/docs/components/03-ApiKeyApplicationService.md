# ApiKeyApplicationService

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.billing.application.ApiKeyApplicationService` |
| 对外 Controller | `ApiKeyController`（JWT）、`InternalApiKeyController`（内部） |
| TDD | [O-07  API Key 生命周期](../../docs/TDD/O-07-APIKey生命周期-过期与轮换.md)；网关缓存见 [O-05](../../docs/TDD/O-05-Redis缓存-APIKey解析与余额.md) |

---

## 1. 背景

平台 API Key 形如 `sk_tokenhub_<hex>`，**明文仅创建时返回一次**；库内仅存 **SHA-256 指纹**。网关对 `/v1/**` 将 Bearer 当作 Key 时，计算指纹后调 billing 内部解析接口，注入 `X-User-Id` / `X-Api-Key-Id`。

billing 是 Key 状态的**权威数据源**；网关 Redis 缓存（O-5）仅为读加速，失效后仍回落本服务。

---

## 2. 作用

| 方法 | 说明 |
|------|------|
| `create(userId, name)` / `create(..., ttlDays)` | 生成 Key、写库 ACTIVE；可选 `expires_at`（O-7） |
| `listForUser` | 列表（不含明文） |
| `disable` | 置 `DISABLED`，校验归属 |
| `requireActiveByFingerprint` | 内部严格解析，无效抛 `UNAUTHORIZED` |
| `findActiveByFingerprint` | 内部宽松解析，无效返回 `Optional.empty()` |
| `isActiveAndUnexpired` | `ACTIVE` 且 `expires_at IS NULL OR > now` |

创建 Key 前调用 `accountBalanceApplicationService.getOrCreate(userId)`，保证账户行存在。

---

## 3. 触发条件

| 入口 | 调用链 |
|------|--------|
| `POST /apikeys` | JWT → `create` |
| `GET /apikeys`、`PATCH /apikeys/{id}` | JWT → `list` / `disable` |
| `GET /internal/api-keys/by-fingerprint/{fingerprint}` | Internal Token → `findActiveByFingerprint` |

定时任务 `ApiKeyExpirationScheduler` 将到期 ACTIVE 翻转为 **EXPIRED**（最终收敛，见 [08-Schedulers.md](./08-Schedulers.md)）。

---

## 4. 实现要点

**前缀与指纹：**

```20:58:billing-service/src/main/java/com/tokenhub/billing/application/ApiKeyApplicationService.java
  private static final String KEY_PREFIX = "sk_tokenhub_";
  // ...
    String plaintext = KEY_PREFIX + HexFormat.of().formatHex(rnd);
    String fingerprint = ApiKeySupport.sha256HexUtf8(plaintext);
    // ...
    if (ttlDays != null && ttlDays > 0) {
      row.setExpiresAt(LocalDateTime.now().plusDays(ttlDays));
    }
```

**可用性判定（O-7）：**

```108:115:billing-service/src/main/java/com/tokenhub/billing/application/ApiKeyApplicationService.java
  static boolean isActiveAndUnexpired(ApiKeyPo row) {
    if (row == null || !"ACTIVE".equalsIgnoreCase(row.getStatus())) {
      return false;
    }
    LocalDateTime expiresAt = row.getExpiresAt();
    return expiresAt == null || expiresAt.isAfter(LocalDateTime.now());
  }
```

即时校验在解析路径完成；Scheduler 负责 DB 状态与运营可见性一致。

---

## 5. 数据模型（概要）

表 `api_keys`（`ApiKeyPo`）：`user_id`、`name`、`fingerprint`、`status`（`ACTIVE` / `DISABLED` / `EXPIRED`）、`expires_at`、`created_at`。

---

## 6. 优劣分析

| 优点 | 缺点 |
|------|------|
| 不落库明文，泄露面小 | 丢失明文无法恢复，只能轮换 |
| 指纹查询 O(1) 索引友好 | 高 QPS 解析仍打 DB（靠网关 O-5 缓释） |
| 过期双轨：即时 + 定时收敛 | `ttlDays` 创建入口尚未暴露到所有 Controller DTO |

---

## 7. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **只存 Argon2/bcrypt 哈希** | 防离线撞库（指纹已不可逆，风险较低） |
| **Key 分片前缀** | `sk_live_` / `sk_test_` 环境隔离 |
| **HSM / Vault 代管** | 企业级轮换与审计 |
| **短期 Token + 长期 Key** | 降低长期 Key 暴露面 |

---

## 8. 配置项

| 配置 | 默认 | 说明 |
|------|------|------|
| `tokenhub.billing.apikey-expiration.enabled` | `false` | 到期扫描 Job |
| `tokenhub.billing.apikey-expiration.cron` | `0 */5 * * * ?` | 每 5 分钟 |

---

## 9. 相关文档

- [08-Schedulers.md](./08-Schedulers.md)
- [01-InternalApiGuardFilter.md](./01-InternalApiGuardFilter.md)
- [gateway filters/03-BillingApiKeyResolveGatewayFilter.md](../../gateway-service/docs/filters/03-BillingApiKeyResolveGatewayFilter.md)
- TDD：[O-07](../../docs/TDD/O-07-APIKey生命周期-过期与轮换.md)、[O-05](../../docs/TDD/O-05-Redis缓存-APIKey解析与余额.md)
