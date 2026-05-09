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

- `gateway-service`：只做接入编排，不承载复杂领域规则
- `user-center-service`：用户认证与账户域
- `adapter-service`：供应商协议适配与转换
- `billing-service`：计费与账务域
- `payment-service`：支付与对账域
- `ops-console`：管理后台 BFF/接口

## 违规处理（棘轮）

1. 若扫描到历史违规，写入 `tests/architecture/known_violations.json`。
2. 后续 PR 不允许新增违规。
3. 每次修复违规都应同步减少基线文件。

## 部署制品（与分层的关系）

Java 分层约束仅作用于业务代码目录；**Dockerfile、Compose、初始化 SQL、环境示例文件** 等部署制品放在 `deploy/` 与各服务构建目录中，随本仓库版本发布，不在 `presentation/application/domain/infrastructure` 四层之内。

## 本仓库机械化校验

- 本地：`python scripts/check_boundaries.py`
- CI：`.github/workflows/ci.yml`
