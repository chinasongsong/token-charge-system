# 阶段交付总结

> **强制**：每完成 `docs/dev-plan.md` 中的一个阶段（P0–P8），必须在**同一提交或紧随其后的提交**中更新本文件对应章节。详见 [`AGENTS.md`](./AGENTS.md)「阶段交付总结（强制）」。
>
> **写法**：用完整句子说明本阶段**做了什么**、**关键路径/模块**、**如何验收**、**遗留或下一阶段的输入**。避免只写「已完成」而无实质信息。

---

## P0 工程地基

**完成状态**：已完成  

### 本阶段做了什么

- 建立 Maven 多模块（根 BOM、JDK 21 Enforcer）、公共库 `common-core` / `common-web` / `common-security` / `common-mybatis` 与各业务服务空壳（四层包 + `Application`）。
- 统一 HTTP 信封 `ApiResponse`、错误码、Trace、`GlobalExceptionHandler`、JWT/ApiKey 工具类；Compose 挂载 `deploy/sql`；提供 `V1__core_schema.sql`、OpenAPI 骨架、`console-web`（Vue3+Vite）脚手架。
- 各服务多阶段 `Dockerfile`；脚本 `check_boundaries.py`、`gc_scan.py` 与 CI 可对齐。

### 关键交付物（路径）

根 `pom.xml`、`common/**`、`gateway-service` / `user-*` / `adapter-*` / `billing-*` / `payment-*` / `ops-console`、`console-web/`、`deploy/sql/V1__core_schema.sql`、`deploy/docker-compose.yml`、`docs/openapi/unified-api.yaml`。

### 验收与检查

- `mvn package`（JDK 21）；Compose 起 `mysql`/`redis`；边界检查与 GC 扫描通过。

### 遗留 / 下一阶段的输入

- 业务接口待 P1 起在各服务落地；网关路由待 P2。

---

## P1 用户与认证（user-center-service）

**完成状态**：已完成  

### 本阶段做了什么

- `users`、`user_devices`、`password_reset_codes`（`V2`）与 MyBatis-Plus 仓储实现；领域层无框架依赖。
- `UserApplicationService`：注册/登录/JWT（HS256，`JWT_SECRET`）、BCrypt、忘记/重置密码（验证码 SHA-256 落库 + 日志模拟邮件）、登录设备轨迹写入 `user_devices`。
- `GET /user/me` 走 MVC 拦截器校验 Bearer JWT；`POST /user/register`、`/login`、`/logout`、`/password/forgot`、`/password/reset`。
- Application 层单测 + Jacoco 对 `UserApplicationService` 行覆盖率门禁；`GlobalExceptionHandler` 对齐 HTTP 状态码（如 401）。

### 关键交付物（路径）

`user-center-service/src/main/java`（`domain` / `application` / `infrastructure.persistence` / `presentation`）、`deploy/sql/V2__password_reset_codes.sql`、扩展后的 `docs/openapi/unified-api.yaml`、`user-center-service/src/main/resources/application.yml`。

### 验收与检查

- curl：注册 → 登录 → Bearer 访问 `/user/me`；`python scripts/check_boundaries.py` 通过。

### 遗留 / 下一阶段的输入

- 真实邮件通道替换 `VerificationMailPort`；网关统一入口待 P2。

---

## P2 网关 + 首家供应商（DeepSeek）

**完成状态**：未开始  

### 本阶段做了什么

（阶段完成后填写：网关路由、`DeepSeekAdapter`、`/v1/chat/completions` 非流式、过滤器与配置等。）

### 关键交付物（路径）

### 验收与检查

### 遗留 / 下一阶段的输入

---

## P3 计费 MVP

**完成状态**：未开始  

### 本阶段做了什么

### 关键交付物（路径）

### 验收与检查

### 遗留 / 下一阶段的输入

---

## P4 第二家供应商 + 路由

**完成状态**：未开始  

### 本阶段做了什么

### 关键交付物（路径）

### 验收与检查

### 遗留 / 下一阶段的输入

---

## P5 支付与套餐

**完成状态**：未开始  

### 本阶段做了什么

### 关键交付物（路径）

### 验收与检查

### 遗留 / 下一阶段的输入

---

## P6 C 端控制台

**完成状态**：未开始  

### 本阶段做了什么

### 关键交付物（路径）

### 验收与检查

### 遗留 / 下一阶段的输入

---

## P7 运营后台 + 风控 + 可观测

**完成状态**：未开始  

### 本阶段做了什么

### 关键交付物（路径）

### 验收与检查

### 遗留 / 下一阶段的输入

---

## P8 SSE 流式 + 客服 + 验收

**完成状态**：未开始  

### 本阶段做了什么

### 关键交付物（路径）

### 验收与检查

### 遗留 / 下一阶段的输入
