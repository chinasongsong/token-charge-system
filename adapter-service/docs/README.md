# adapter-service 文档

模型供应商适配层：OpenAI 兼容 API、加权路由、熔断与故障转移、用量结算回调 billing。

## 阅读顺序

| 顺序 | 文档 | 内容 |
|------|------|------|
| 1 | [00-模块总览.md](./00-模块总览.md) | 数据流、供应商、与网关头约定 |
| 2 | [components/01-OpenAiCompatibleController.md](./components/01-OpenAiCompatibleController.md) | `/v1` HTTP 面 |
| 3 | [components/02-FailoverRoutingAdapter.md](./components/02-FailoverRoutingAdapter.md) | 加权首跳 + 故障转移 |
| 4 | [components/03-WeightedRoutingPolicy.md](./components/03-WeightedRoutingPolicy.md) | 路由权重 |
| 5 | [components/04-ProviderAdapters.md](./components/04-ProviderAdapters.md) | DeepSeek / 智谱适配器 |
| 6 | [components/05-BillingSettlementClient.md](./components/05-BillingSettlementClient.md) | 用量记账 |
| 7 | [components/07-ChatIdempotencyResponseCache.md](./components/07-ChatIdempotencyResponseCache.md) | Chat 响应幂等缓存（用户+平台） |
| 8 | [components/06-RiskEventRecorder.md](./components/06-RiskEventRecorder.md) | 故障转移审计 |
| 9 | [08-路由与配置.md](./08-路由与配置.md) | Resilience4j、环境变量 |

## 源码入口

| 类型 | 路径 |
|------|------|
| 控制器 | `presentation/OpenAiCompatibleController.java` |
| 路由 | `infrastructure/routing/*` |
| 供应商 | `infrastructure/deepseek`, `infrastructure/zhipu` |
| 计费客户端 | `infrastructure/billing/BillingSettlementClient.java` |
