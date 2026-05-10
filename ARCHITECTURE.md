# ARCHITECTURE.md

## 系统目标

构建一个统一聚合国内模型 API 的中转平台，对外暴露一致协议，对内适配多供应商，并提供 C 端用户控制台、计费、风控与运营能力。

## 逻辑架构

1. **接入层**：`gateway-service`  
   负责鉴权、限流、路由、协议兼容、流式转发。
2. **应用层**：`user-center-service`、`billing-service`、`payment-service`  
   负责业务编排、用例处理、事务边界。
3. **领域层**：各服务 `domain`  
   负责规则与实体，不依赖框架技术实现。
4. **基础设施层**：各服务 `infrastructure`  
   负责 DB、Redis、MQ、第三方 API 适配。
5. **适配层**：`adapter-service`  
   负责供应商协议转换和错误归一。

## 核心数据流

1. 用户调用统一接口进入网关。
2. 网关完成鉴权、限流和路由决策。
3. 请求进入适配器并转发到供应商。
4. 响应回传后触发计费、流水、审计事件。
5. 控制台消费聚合数据进行展示。

## 后端与前端模块布局（Maven / P0）

| 模块路径 | Maven `artifactId` | 主要职责 |
|-----------|---------------------|----------|
| `common/common-core` | `common-core` | `ApiResponse`、`ErrorCode`、`BusinessException`、`TraceContext`、`TraceIdInterceptor`（供 MVC 链路选用） |
| `common/common-web` | `common-web` | `TraceBootstrapFilter`、全局异常处理、请求日志过滤器 |
| `common/common-security` | `common-security` | `JwtSupport`、`ApiKeySupport`（无业务耦合） |
| `common/common-mybatis` | `common-mybatis` | MyBatis-Plus 与驱动版本对齐（后续各服务按需依赖） |
| `gateway-service` | `gateway-service` | Spring Cloud Gateway 接入层（与 WebMVC 隔离，不依赖 `common-web`） |
| `user-center-service` | `user-center-service` | 用户与认证：注册/登录/JWT、`users` / `user_devices` / 密码重置码表、设备登录轨迹（P1 已实现 HTTP 面） |
| `adapter-service` | `adapter-service` | 供应商适配（**P2 起首家 DeepSeek / deepseek-v4**；P4 第二家 DashScope 或智谱双活） |
| `billing-service` | `billing-service` | **P3**：API Key、余额、`model_prices` 计价、`request_orders`/`usage_ledger`；内部 `/internal/**` 供网关/适配器 |
| `payment-service` | `payment-service` | 支付与对账（P5 起） |
| `ops-console` | `ops-console` | 运营控制面（P7 起） |
| `console-web/` | （npm `console-web`） | C 端控制台（Vue 3 + Vite + TS，P6 丰富页面） |

根 `pom.xml` 统一 **JDK 21**、Spring Boot `3.3.6`、Spring Cloud `2023.0.5` BOM；`maven-enforcer-plugin` 要求构建 JDK ≥ 21。

## 部署与制品（Docker）

- **默认交付形态**：各服务以容器镜像发布，通过 Docker Compose（或上层编排）一键拉起依赖与应用；镜像构建定义与各服务源码同仓维护。
- **内置资源随仓**：与本项目相关的部署与运行所需「可版本化」资产统一放在仓库内，包括但不限于：`deploy/`（Compose、编排片段、入口脚本）、各服务的 `Dockerfile` / 多阶段构建、`docker/` 构建上下文、初始化 SQL、网关/前端的默认静态与路由配置模板、`docs/openapi/` 等契约与文档。
- **密钥与环境的边界**：供应商密钥、支付密钥、生产数据库口令等**不得**提交入库；仓库内仅保留 `deploy/env.example` 这类 **示例与占位**，运行时通过环境变量或挂载外部密钥注入（见 `docs/SECURITY.md`）。
- **本地与 CI**：本地开发与流水线可使用同一套 Compose 骨架做冒烟；具体服务名与镜像标签随多模块工程落地后补齐。

## 架构守护

- 机械约束：`scripts/check_boundaries.py`
- 基线文件：`tests/architecture/known_violations.json`
- CI 执行：`.github/workflows/ci.yml`
- 漂移扫描：`scripts/gc_scan.py` + `.github/workflows/gc.yml`
