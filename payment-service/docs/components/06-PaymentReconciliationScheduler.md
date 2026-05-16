# PaymentReconciliationScheduler

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.payment.infrastructure.schedule.PaymentReconciliationScheduler` |
| 类型 | `@Scheduled` 定时任务 |
| 默认 | **关闭**（`reconcile-enabled=false`） |

---

## 1. 背景

支付系统中长期停留在 **INIT** 的订单可能表示：用户未付款、回调丢失、或渠道延迟。自动把 INIT 改为 PAID 会在「未真实收款」时造成 **盗刷式入账**，因此本调度器**只观测、不入账**。

业界做法：对账任务 + 告警 + 人工/半自动补偿（retry），而非 blind auto-credit。

---

## 2. 作用

1. 当 `tokenhub.payment.reconcile-enabled=true` 时按 cron 执行。
2. 查询最多 **500** 条 `status=INIT` 订单。
3. 统计 `created_at` 早于阈值（默认 60 分钟）的 **stale** 数量。
4. 若 `stale > 0`：**WARN** 日志；否则 **INFO** 日志。

**不调用** `BillingCreditClient`，不修改订单状态。

---

## 3. 实现要点

```35:60:payment-service/src/main/java/com/tokenhub/payment/infrastructure/schedule/PaymentReconciliationScheduler.java
  @Scheduled(cron = "${tokenhub.payment.reconcile-cron:0 */10 * * * ?}")
  public void reportPendingInitOrders() {
    if (!enabled) {
      return;
    }
    List<PaymentOrderPo> pending =
        paymentOrderMapper.selectList(
            new LambdaQueryWrapper<PaymentOrderPo>()
                .eq(PaymentOrderPo::getStatus, "INIT")
                .last("LIMIT 500")
        );
    // ... stale count by createdAt vs threshold ...
    if (stale > 0) {
      log.warn(
          "payment reconcile: INIT orders total={}, olderThan{}Minutes={} (review channel state; retry credit via internal API if paid)",
          ...
      );
    }
  }
```

---

## 4. 运维 playbook（INIT 积压）

| 步骤 | 动作 |
|------|------|
| 1 | 查 WARN 日志中的 `total` / `olderThan*Minutes` |
| 2 | 在渠道后台确认是否已收款 |
| 3a | 已收款 → `POST /internal/payments/orders/retry-credit` 或 `channel-reconcile` |
| 3b | 未收款 → 关闭或等待用户支付 |
| 4 | 批量差异 → CSV 对账 `ChannelReconciliationApplicationService` |

---

## 5. 配置项

| 配置键 | 环境变量 | 默认 | 说明 |
|--------|----------|------|------|
| `tokenhub.payment.reconcile-enabled` | `PAYMENT_RECONCILE_ENABLED` | `false` | 是否启用 |
| `tokenhub.payment.reconcile-cron` | `PAYMENT_RECONCILE_CRON` | `0 */10 * * * ?` | 每 10 分钟 |
| `tokenhub.payment.reconcile-stale-init-warn-minutes` | `PAYMENT_RECONCILE_STALE_INIT_WARN_MINUTES` | `60` | stale 阈值（分钟） |

---

## 6. 相关文档

- [05-ChannelReconciliation.md](./05-ChannelReconciliation.md)
- [03-PaymentExecutionService.md](./03-PaymentExecutionService.md)
- [08-路由与配置.md](../08-路由与配置.md)
