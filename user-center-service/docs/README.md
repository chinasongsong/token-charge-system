# user-center-service 文档

用户与认证服务：注册、登录、JWT 签发、密码重置、设备登录轨迹、工单（Support）。

## 阅读顺序

| 顺序 | 文档 | 内容 |
|------|------|------|
| 1 | [00-模块总览.md](./00-模块总览.md) | 分层、请求链路、与网关关系 |
| 2 | [components/01-UserJwtMvcInterceptor.md](./components/01-UserJwtMvcInterceptor.md) | JWT 拦截与受保护路径 |
| 3 | [components/02-UserApplicationService.md](./components/02-UserApplicationService.md) | 注册/登录/重置密码 |
| 4 | [components/03-JwtAccessTokenIssuer.md](./components/03-JwtAccessTokenIssuer.md) | 令牌签发 |
| 5 | [components/04-SupportTicketApplicationService.md](./components/04-SupportTicketApplicationService.md) | 用户工单 |
| 6 | [08-路由与配置.md](./08-路由与配置.md) | 端口、环境变量、API 列表 |

## 源码入口

| 类型 | 路径 |
|------|------|
| 启动类 | `src/main/java/com/tokenhub/usercenter/UserCenterApplication.java` |
| HTTP | `presentation/*Controller.java` |
| 用例 | `application/*ApplicationService.java` |
| 领域 | `domain/user`, `domain/auth` |
| 配置 | `src/main/resources/application.yml` |

## 相关文档

- 网关 JWT 验签：[gateway-service/docs/filters/02-V1IngressAuthGatewayFilter.md](../../gateway-service/docs/filters/02-V1IngressAuthGatewayFilter.md)
- 分层：`docs/architecture/LAYERS.md`
