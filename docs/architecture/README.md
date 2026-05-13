# 架构文档索引

本目录存放与**整体技术架构、分层与部署**相关的说明，与根目录 [`ARCHITECTURE.md`](../../ARCHITECTURE.md)（精简全景）互补。

| 文档 | 说明 |
|------|------|
| [整体技术架构](./整体技术架构.md) | 十三章节完整架构说明：背景、分层总图、领域拆分、应用/数据/中间件、部署、非功能、外部依赖、运维与演进 |
| [技术负债与路线图](./技术负债与路线图.md) | **O-1～O-9** 总表：状态、TDD 链接、目标阶段、验收要点与收口流程（与 `dev-plan.md` §2.3 对齐） |
| [LAYERS.md](./LAYERS.md) | 各服务 `presentation / application / domain / infrastructure` 依赖规则与边界 |

**上游依据**：[`plan.md`](../../plan.md)、[`docs/dev-plan.md`](../dev-plan.md)、[`docs/SECURITY.md`](../SECURITY.md)。

**深度设计（优化项 O-1～O-9）**：[`docs/TDD/`](../TDD/README.md)。
