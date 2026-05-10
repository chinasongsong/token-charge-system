# AGENTS.md

## 项目定位

本仓库用于构建 Java 模型 API 中转平台（面向 C 端与开发者双场景），当前处于工程初始化阶段。

## Agent 工作入口

- 业务与产品基线：`plan.md`
- 架构全景：`ARCHITECTURE.md`
- 分层规则：`docs/architecture/LAYERS.md`
- 安全基线：`docs/SECURITY.md`
- 工程原则：`docs/golden-principles/README.md`

## 目录约定（目标态）

- `gateway-service/`：统一网关与协议兼容
- `user-center-service/`：用户与认证
- `adapter-service/`：模型供应商适配
- `billing-service/`：计费与账务
- `payment-service/`：充值与支付回调
- `ops-console/`：运营管理后台
- `console-web/`：C 端用户前台
- `deploy/`：Docker Compose、部署脚本与环境示例（密钥不入库）
- `docs/`：架构与治理文档
- `tests/architecture/`：分层边界测试
- `scripts/`：工程守护脚本

## 分层约束（强制）

1. `gateway` 只能依赖 `application` 与 `domain`，不能直连 `infrastructure` 细节。
2. `application` 可以依赖 `domain`，不能依赖 `presentation`。
3. `domain` 不得依赖外部框架实现（Spring/HTTP/DB）。
4. `infrastructure` 可依赖 `application` 与 `domain`，用于技术实现落地。
5. 模块间禁止循环依赖。

详情见 `docs/architecture/LAYERS.md`。

## 开发流程

1. 先更新文档与接口契约，再落代码。
2. 新增模块必须声明层级并纳入边界检查。
3. 提交前执行：
   - `python scripts/check_boundaries.py`
   - `python scripts/gc_scan.py`

## 质量门禁

- CI 会执行架构边界检查与基础熵扫描。
- 若存在历史违规，请写入 `tests/architecture/known_violations.json`，只能减少不能新增。

## Agent 输出要求

- 任何结构性改动需同步更新：
  - `ARCHITECTURE.md`
  - `docs/architecture/LAYERS.md`
- 新增安全相关能力需同步更新 `docs/SECURITY.md`。


## Agent 总结要求

- 每次阶段性开发完成后，需对本次修改做一个总结，并同步更新：
    - `PROGRESS.md`
