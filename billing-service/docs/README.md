# billing-service 文档

计费与账务：API Key、余额、计价结算、预检/预占、Outbox、对账调度、控制台查询 API。

## 阅读顺序

| 顺序 | 文档 | 内容 |
|------|------|------|
| 1 | [00-模块总览.md](./00-模块总览.md) | 内外 API、事务边界、与网关/adapter/payment |
| 2 | [components/01-InternalApiGuardFilter.md](./components/01-InternalApiGuardFilter.md) | 内部接口令牌 |
| 3 | [components/02-BillingJwtMvcInterceptor.md](./components/02-BillingJwtMvcInterceptor.md) | 控制台 JWT |
| 4 | [components/03-ApiKeyApplicationService.md](./components/03-ApiKeyApplicationService.md) | Key 生命周期 |
| 5 | [components/04-AccountBalanceApplicationService.md](./components/04-AccountBalanceApplicationService.md) | 余额借贷 |
| 6 | [components/05-BillingSettlementApplicationService.md](./components/05-BillingSettlementApplicationService.md) | 按量结算 O-1/O-2 |
| 7 | [components/06-BalanceReservationApplicationService.md](./components/06-BalanceReservationApplicationService.md) | 预占 O-3 |
| 8 | [components/07-SettlementOutboxScheduler.md](./components/07-SettlementOutboxScheduler.md) | 异步 Outbox |
| 9 | [components/08-Schedulers.md](./components/08-Schedulers.md) | Key 过期、账务对账 |
| 10 | [08-路由与配置.md](./08-路由与配置.md) | 配置项与 API 表 |

## 相关 TDD

- [O-01](../../docs/TDD/O-01-扣费并发-分布式锁与悲观锁策略.md)、[O-02](../../docs/TDD/O-02-异步账务-Outbox与MQ.md)、[O-03](../../docs/TDD/O-03-预占额度与冲正-流式联动.md)、[O-05](../../docs/TDD/O-05-Redis缓存-APIKey解析与余额.md)、[O-07](../../docs/TDD/O-07-APIKey生命周期-过期与轮换.md)
