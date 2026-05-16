# payment-service 文档

支付与充值：Mock 渠道、回调签验、幂等入账 billing、渠道对账、INIT 订单观测。

## 阅读顺序

| 顺序 | 文档 | 内容 |
|------|------|------|
| 1 | [00-模块总览.md](./00-模块总览.md) | 充值状态机、与 billing 协作 |
| 2 | [components/01-PaymentInternalApiGuardFilter.md](./components/01-PaymentInternalApiGuardFilter.md) | 内部 API |
| 3 | [components/02-PaymentJwtMvcInterceptor.md](./components/02-PaymentJwtMvcInterceptor.md) | 用户 JWT（Mock 充值面） |
| 4 | [components/03-PaymentExecutionService.md](./components/03-PaymentExecutionService.md) | 订单与入账 |
| 5 | [components/04-PaymentCallbackApplicationService.md](./components/04-PaymentCallbackApplicationService.md) | 回调处理 |
| 6 | [components/05-ChannelReconciliation.md](./components/05-ChannelReconciliation.md) | 渠道对账导入 |
| 7 | [components/06-PaymentReconciliationScheduler.md](./components/06-PaymentReconciliationScheduler.md) | INIT 观测 |
| 8 | [08-路由与配置.md](./08-路由与配置.md) | API 与环境变量 |
