# AGENTS.md

## 项目定位

本仓库用于构建 Java 模型 API 中转平台（面向 C 端与开发者双场景），当前处于工程初始化阶段。

## Agent 工作入口

- 执行路线图与阶段任务：`docs/dev-plan.md`（**唯一权威阶段划分**）
- 阶段交付总结（每阶段 Done 后必填）：根目录 **`PROGRESS.md`**
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
- `PROGRESS.md`（仓库根）：各阶段交付总结，与 `docs/dev-plan.md` 勾选同步
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

## 阶段交付总结（强制）

以下与 `docs/dev-plan.md` 中「每阶段闭环」**并列**，缺一视为阶段未收口：

1. **何时写**：在 `docs/dev-plan.md` 中将某一阶段（Px）任务勾选为完成、或通过该阶段验收标准的**同一轮改动**中，必须更新 **`PROGRESS.md`**。
2. **写什么**：打开 `PROGRESS.md`，找到对应 **`## Px …`** 章节，填写（不要用占位符敷衍）：
   - **本阶段做了什么**（功能与架构层面的实质交付）；
   - **关键交付物（路径）**（仓库内路径，便于 Code Review）；
   - **验收与检查**（跑过哪些命令/用例、门禁是否绿）；
   - **遗留 / 下一阶段的输入**（若有）。
3. **未完成阶段**：可将「完成状态」标为「未开始」或「进行中」，但**不得在已完成阶段留空模板**。
4. **对用户可见的回合小结**：除更新 `PROGRESS.md` 外，建议在对话结束时用简短列表归纳本阶段变更（与 `PROGRESS.md` 一致，避免两套说法）。

未更新 `PROGRESS.md` 即宣称阶段完成，属于**流程不合规**，应在后续提交中补全。
