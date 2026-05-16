# 定时任务（Schedulers）

| 项 | 内容 |
|----|------|
| 启用 | `@EnableScheduling` on `BillingApplication` |
| 组件 | `ApiKeyExpirationScheduler`、`BillingReconciliationScheduler` |
| 另见 | [07-SettlementOutboxScheduler.md](./07-SettlementOutboxScheduler.md)（`fixedDelay` 轮询，非 cron） |

---

## 1. ApiKeyExpirationScheduler（O-7）

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.billing.infrastructure.schedule.ApiKeyExpirationScheduler` |
| TDD | [O-07 API Key 生命周期](../../docs/TDD/O-07-APIKey生命周期-过期与轮换.md) |

### 作用

将 **`expires_at < now` 且 `status=ACTIVE`** 的 Key 批量更新为 **`EXPIRED`**（`ApiKeyLifecycleMapper.markExpired`）。

### 与在线解析的关系

`ApiKeyApplicationService.isActiveAndUnexpired` 在 **解析时即时拒绝** 已过期 Key；本任务做 **DB 状态最终收敛** 与运营/审计可见性，不替代在线校验。

网关 `BillingApiKeyResolveGatewayFilter` 与 billing `findActiveByFingerprint` 均已尊重 `expires_at`。

### 调度与开关

| 配置 | 默认 |
|------|------|
| `tokenhub.billing.apikey-expiration.enabled` | `false` |
| `tokenhub.billing.apikey-expiration.cron` | `0 */5 * * * ?`（每 5 分钟） |

```31:39:billing-service/src/main/java/com/tokenhub/billing/infrastructure/schedule/ApiKeyExpirationScheduler.java
  @Scheduled(cron = "${tokenhub.billing.apikey-expiration.cron:0 */5 * * * ?}")
  public void sweep() {
    if (!enabled) {
      return;
    }
    int flipped = apiKeyLifecycleMapper.markExpired(LocalDateTime.now());
```

---

## 2. BillingReconciliationScheduler

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.billing.infrastructure.schedule.BillingReconciliationScheduler` |
| 目的 | 平台内 **订单 vs 流水** 一致性巡检（非支付渠道对账） |

### 检查项

| 指标 | 含义 |
|------|------|
| `completedMissingUsageLedger` | `COMPLETED` 订单缺 `usage_ledger` |
| `stalePending` | `PENDING` 超过 `reconciliation-stale-pending-hours` 仍未完成 |

异常时 `log.warn`；正常 `log.info`。

### 调度与开关

| 配置 | 默认 |
|------|------|
| `tokenhub.billing.reconciliation-enabled` | `false` |
| `tokenhub.billing.reconciliation-cron` | `0 0 3 * * ?`（每天 03:00） |
| `tokenhub.billing.reconciliation-stale-pending-hours` | `1` |

```31:51:billing-service/src/main/java/com/tokenhub/billing/infrastructure/schedule/BillingReconciliationScheduler.java
  @Scheduled(cron = "${tokenhub.billing.reconciliation-cron:0 0 3 * * ?}")
  public void reconcileDaily() {
    if (!enabled) {
      return;
    }
    // countCompletedMissingUsageLedger / countStalePending
```

微信/支付宝账单比对在生产接通道后扩展（类注释）。

---

## 3. SettlementOutboxScheduler（交叉引用）

| 类型 | cron / delay |
|------|----------------|
| Outbox `drain` | `fixedDelay` 2000ms（非 cron） |
| 开关 | `tokenhub.billing.outbox.enabled` |

详见 [07-SettlementOutboxScheduler.md](./07-SettlementOutboxScheduler.md)。

---

## 4. 调度总览

```mermaid
gantt
  title billing-service 定时任务（启用后）
  dateFormat X
  axisFormat %s

  section Cron
  ApiKeyExpiration     :0, 300
  Reconciliation 03:00 :crit, 3600

  section FixedDelay
  Outbox drain 2s      :0, 2000
```

---

## 5. 运维建议

| 任务 | 建议 |
|------|------|
| Key 过期 | 生产开启 `apikey-expiration.enabled`；监控 `flipped` 日志 |
| 对账 | 开启 `reconciliation-enabled`；告警 `WARN` 行 |
| Outbox | 与 MQ 同步开启；监控 `FAILED` 与 `attempts` |

多实例部署：cron 任务可能重复执行；Key 过期 `UPDATE` 应幂等；Outbox `claimPendingBatch` 需 DB 层防重（实现见 Mapper）。

---

## 6. 相关文档

- [03-ApiKeyApplicationService.md](./03-ApiKeyApplicationService.md)
- [05-BillingSettlementApplicationService.md](./05-BillingSettlementApplicationService.md)
- [07-SettlementOutboxScheduler.md](./07-SettlementOutboxScheduler.md)
- TDD：[O-07](../../docs/TDD/O-07-APIKey生命周期-过期与轮换.md)
