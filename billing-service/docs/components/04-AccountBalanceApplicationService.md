# AccountBalanceApplicationService

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.billing.application.AccountBalanceApplicationService` |
| 表 | `account_balance`（`AccountBalancePo`，`@Version` 乐观锁） |
| 关联 | 结算扣款、预检、预占、幂等充值 |

---

## 1. 背景

用户 Token 余额是计费基础。账户按 `user_id` 一行，货币默认 `TOKEN`，金额为 **micro 单位**（与 `PricingService` 计价一致）。并发更新依赖 MyBatis-Plus **`@Version` 乐观锁**；冲突时应用层最多重试 8 次。

可选 **O-1** 用户维度 Redis 短锁在 `BillingSettlementFacade` 外围串行化结算，与乐观锁、幂等键形成组合（见 [05-BillingSettlementApplicationService.md](./05-BillingSettlementApplicationService.md)）。

---

## 2. 作用

| 方法 | 说明 |
|------|------|
| `getOrCreate(userId)` | 懒创建余额行；并发 `DuplicateKeyException` 后重读 |
| `getBalance` | 读余额 |
| `debit(userId, amount)` | 扣款；不足 → `BALANCE_INSUFFICIENT`；版本冲突重试 |
| `credit(userId, amount)` | 加款 |
| `creditIdempotent(userId, amount, sourceRef)` | 支付回调入账，`balance_topup_receipt` 幂等 |
| `assertPositiveBalance(userId)` | 余额 &gt; 0，否则 `BALANCE_INSUFFICIENT`（网关预检） |

---

## 3. 触发条件

| 入口 | 方法 |
|------|------|
| `POST /internal/billing/preflight` | `assertPositiveBalance` |
| `POST /internal/billing/credit` | `creditIdempotent` |
| `BillingSettlementApplicationService#settle` | `debit` |
| `BalanceReservationApplicationService#reserve` | `getOrCreate` + 可用余额计算 |
| `ApiKeyApplicationService#create` | `getOrCreate` |
| `POST /billing/account/mock-deposit`（若开启） | `credit` |

---

## 4. 实现要点

**乐观锁扣款：**

```52:68:billing-service/src/main/java/com/tokenhub/billing/application/AccountBalanceApplicationService.java
  @Transactional
  public void debit(long userId, long amount) {
    if (amount <= 0) {
      return;
    }
    for (int attempt = 0; attempt < 8; attempt++) {
      AccountBalancePo row = getOrCreate(userId);
      if (row.getBalance() < amount) {
        throw new BusinessException(ErrorCode.BALANCE_INSUFFICIENT, "余额不足");
      }
      row.setBalance(row.getBalance() - amount);
      int rows = accountBalanceMapper.updateById(row);
      if (rows == 1) {
        return;
      }
    }
    throw new BusinessException(ErrorCode.CONFLICT, "扣款冲突，请重试");
  }
```

`AccountBalancePo` 含 `@Version private Long version`，更新失败时 `rows != 1` 触发重试。

**幂等充值：**

```90:99:billing-service/src/main/java/com/tokenhub/billing/application/AccountBalanceApplicationService.java
  public void creditIdempotent(long userId, long amount, String sourceRef) {
    // ...
    int n = balanceTopupReceiptMapper.insertIgnore(sourceRef, userId, amount);
    if (n == 0) {
      return;
    }
    credit(userId, amount);
  }
```

---

## 5. 与预占（O-3）的关系

`reserve` 计算可用余额：

`available = balance - sum(active RESERVED 且未过期)`

预占**不**直接改 `account_balance` 行；释放/过期后额度回到可用池。实际扣款仍在 `settle` → `debit`。

---

## 6. 优劣分析

| 优点 | 缺点 |
|------|------|
| 无锁默认路径简单、可扩展 | 热点用户乐观锁竞争激烈 |
| 幂等充值防重复到账 | 预检仅看余额 &gt; 0，未扣预占（网关预检与流式预占需配合） |
| `getOrCreate` 处理并发建户 | 未实现 O-5 余额只读缓存（有意保持强一致读库） |

---

## 7. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **`SELECT … FOR UPDATE`** | 行锁串行，减少应用重试 |
| **账户分片 + 流水账本** | 热点拆子账户，最终汇总 |
| **Redis 原子扣减 + 异步落库** | 极高 QPS，复杂度高 |
| **双写 + 对账** | 支付行业常见 |

TDD：[O-01](../../docs/TDD/O-01-扣费并发-分布式锁与悲观锁策略.md) 讨论锁与乐观锁并存策略。

---

## 8. 配置项

余额本身无独立开关；相关：

| 配置 | 说明 |
|------|------|
| `tokenhub.billing.allow-mock-deposit` | 是否允许 mock 充值 API |
| `tokenhub.billing.balance-lock.*` | O-1 结算外围锁（见组件 05） |

---

## 9. 相关文档

- [05-BillingSettlementApplicationService.md](./05-BillingSettlementApplicationService.md)
- [06-BalanceReservationApplicationService.md](./06-BalanceReservationApplicationService.md)
- [gateway filters/07-BillingPreflightGatewayFilter.md](../../gateway-service/docs/filters/07-BillingPreflightGatewayFilter.md)
