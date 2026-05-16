# gateway-service 文档

本目录说明 **TokenHub 统一网关**（`gateway-service`）的设计与实现，便于新人「吃透」每个过滤器与路由规则。

## 阅读顺序

| 顺序 | 文档 | 内容 |
|------|------|------|
| 1 | [00-网关总览.md](./00-网关总览.md) | 两阶段处理模型、路由表、请求分类 |
| 2 | [01-TraceGatewayFilter.md](./filters/01-TraceGatewayFilter.md) | 全链路 TraceId |
| 3 | [02-V1IngressAuthGatewayFilter.md](./filters/02-V1IngressAuthGatewayFilter.md) | `/v1/**` 入口鉴权（Bearer / JWT） |
| 4 | [03-BillingApiKeyResolveGatewayFilter.md](./filters/03-BillingApiKeyResolveGatewayFilter.md) | API Key 指纹解析与用户头注入 |
| 5 | [04-ApiKeyResolutionCache.md](./filters/04-ApiKeyResolutionCache.md) | Key 解析 Redis 缓存（O-5） |
| 6 | [05-IpRiskAndQuotaGatewayFilter.md](./filters/05-IpRiskAndQuotaGatewayFilter.md) | IP 黑白名单与日配额（O-6） |
| 7 | [06-ApiKeyRedisRateLimitGatewayFilter.md](./filters/06-ApiKeyRedisRateLimitGatewayFilter.md) | 秒级 QPS 限流 |
| 8 | [07-BillingPreflightGatewayFilter.md](./filters/07-BillingPreflightGatewayFilter.md) | Chat 余额预检 |
| 9 | [08-路由与配置.md](./08-路由与配置.md) | `application.yml`、环境变量、超时 |
| 10 | [09-错误响应与头约定.md](./09-错误响应与头约定.md) | 业务 JSON、注入头、错误码 |

## 源码入口

| 类型 | 路径 |
|------|------|
| 启动类 | `src/main/java/com/tokenhub/gateway/GatewayApplication.java` |
| 全局过滤器 | `src/main/java/com/tokenhub/gateway/infrastructure/web/*GatewayFilter.java` |
| Key 缓存 | `src/main/java/com/tokenhub/gateway/infrastructure/cache/ApiKeyResolutionCache.java` |
| 统一错误 JSON | `src/main/java/com/tokenhub/gateway/infrastructure/json/GatewayJsonResponses.java` |
| 路由与配置 | `src/main/resources/application.yml` |

## 相关仓库文档

- 分层约束：`docs/architecture/LAYERS.md`（网关仅 `infrastructure`，不承载领域逻辑）
- 阶段能力矩阵：`docs/dev-plan.md` §2.1
- 技术负债 O-5 / O-6：`docs/architecture/技术负债与路线图.md`
