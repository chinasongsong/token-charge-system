# LAYERS.md

## 分层定义

采用经典四层模型：

- `presentation`：Controller、HTTP DTO、接口暴露
- `application`：UseCase、Application Service、事务编排
- `domain`：Entity、Value Object、Domain Service、规则
- `infrastructure`：Repository 实现、第三方 SDK、MQ/DB/Cache

## 依赖方向

允许依赖：

- `presentation -> application -> domain`
- `infrastructure -> application, domain`

禁止依赖：

- `domain -> application|presentation|infrastructure`
- `application -> presentation`
- `presentation -> infrastructure`（避免跳过应用层）

## 模块边界（目标）

| 模块 | 典型分层职责 | 共享库 |
|------|----------------|--------|
| `gateway-service` | `presentation`↔路由/过滤器；`application` 编排轻量策略；领域规则保持稀疏 | 仅 JDK/Spring Cloud（避免 servlet stack） |
| `user-center-service` | 清晰拆分注册登录用例：`domain` 无框架依赖，`infrastructure` 承载 MyBatis/JWT 装配 | `common-web`、`common-security`（JWT 密钥仍由环境与配置注入） |
| `adapter-service` | Provider 适配在 `domain`/`application`，HTTP 出站位于 `infrastructure` | `common-web` |
| `billing-service` | 账务不变量放于 `domain`；幂等与流水在 `application` | `common-web`，后续可加 `common-mybatis` |
| `payment-service` | 下单/回调/对账：`application` 管事务，`infrastructure` 接 PSP SDK | `common-web` |
| `ops-console` | RBAC + 管理与审计查询入口 | `common-web` |
| `console-web`（前端） | 视图与路由守卫；经由 OpenAPI 契约对接后端 | N/A |

## 违规处理（棘轮）

1. 若扫描到历史违规，写入 `tests/architecture/known_violations.json`。
2. 后续 PR 不允许新增违规。
3. 每次修复违规都应同步减少基线文件。

## 部署制品（与分层的关系）

Java 分层约束仅作用于业务代码目录；**Dockerfile、Compose、初始化 SQL、环境示例文件** 等部署制品放在 `deploy/` 与各服务构建目录中，随本仓库版本发布，不在 `presentation/application/domain/infrastructure` 四层之内。

## 本仓库机械化校验

- 本地：`python scripts/check_boundaries.py`
- CI：`.github/workflows/ci.yml`
