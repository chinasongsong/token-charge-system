# 部署说明（Docker）

本目录存放与本仓库版本同行的 **可提交** 部署资产：Compose、环境变量示例、后续各服务的镜像构建入口引用等。

## 约定

- **随仓维护**：Compose、`Dockerfile` 引用路径、初始化 SQL（建议放在 `deploy/sql/` 或各服务模块下）、OpenAPI 与网关静态配置模板等均保留在仓库中，便于评审与回滚。
- **密钥不入库**：复制 `env.example` 为本地 `.env`（勿提交），在生产环境用密钥管理或编排注入同名变量。
- **演进**：`docker-compose.yml` 已挂载 `deploy/sql/` 到 MySQL 初始化目录（首次启动会执行目录下 `V1__*.sql`、`V2__*.sql`、`V3__*.sql` 等）；RabbitMQ / Prometheus / Grafana 与各业务服务镜像仍为**注释占位**，按阶段启用。
- **Java 镜像构建**：在各服务目录下提供了多阶段 `Dockerfile`，构建上下文需为**仓库根目录**（参见各文件内注释）。根工程要求 **JDK 21**（`maven-enforcer-plugin`）。

## 快速开始（基础设施）

在项目根目录执行：

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d
```

若尚未创建 `deploy/.env`，可先：`copy deploy\env.example deploy\.env`（Windows）或 `cp deploy/env.example deploy/.env`（Unix），再按需修改口令。

### MySQL / `deploy/sql` 如何初始化

| 场景 | 做法 |
|---|---|
| **全新 MySQL 容器**（`mysql_data` 卷为空或首次 `docker compose up`） | 容器启动时会自动按文件名顺序执行 `deploy/sql/` 下脚本：`V1__*.sql` → `V2__*.sql` → **`V3__seed_model_providers_deepseek.sql`**，无需手工执行。 |
| **已有库**（容器以前跑过，或数据卷里已有数据） | **不会**再次执行初始化目录；新增脚本需手工执行一次，例如在仓库根目录：`mysql -h127.0.0.1 -P3306 -uroot -p token_charge < deploy/sql/V3__seed_model_providers_deepseek.sql`（端口/用户与 `.env` 一致）。也可用任意客户端打开 `V3__*.sql` 执行；该文件含 `ON DUPLICATE KEY UPDATE`，对 `code=deepseek` 可重复执行。 |

## user-center-service（P1 起）

1. 确保 MySQL 已按 `deploy/sql/` 初始化且可连（默认库名 `token_charge`）。
2. 在仓库根执行：`mvn -pl user-center-service spring-boot:run`（需 JDK 21），并设置环境变量 `JWT_SECRET`（≥32 字符）、`MYSQL_JDBC_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD`（或沿用 `application.yml` 默认值仅本地）。
3. 冒烟示例（PowerShell，将 JSON 中的邮箱密码替换为你的值）：

```bash
curl -s -X POST http://localhost:8101/user/register -H "Content-Type: application/json" -d "{\"email\":\"you@example.com\",\"password\":\"password12\",\"displayName\":\"me\"}"
curl -s -X POST http://localhost:8101/user/login -H "Content-Type: application/json" -d "{\"email\":\"you@example.com\",\"password\":\"password12\"}"
curl -s http://localhost:8101/user/me -H "Authorization: Bearer <上一步返回的 accessToken>"
```

## gateway-service + adapter-service（P2 起）

**`load_deploy_env.ps1` 做什么**：把 **`deploy/.env`** 里的 `KEY=VALUE` 写入**当前 PowerShell 进程**的环境变量；之后在同一终端里启动的 **`mvn` / `java` 子进程会继承**这些变量。Spring Boot **不会**自动读取磁盘上的 `.env` 文件。

**是否每次都要执行**：只有在你**新开了一个终端**、且里面还没有这些变量时，才需要先执行一次（或改用下面「一条命令 / IDE」）。**同一终端会话**里点源过一次，后面多次 `spring-boot:run` 一般都能继承，直到关掉窗口。

**能否启动时自动**：

1. **命令行（推荐省事）**：仓库根目录执行 **`.\scripts\run_adapter.ps1`**，内部会先载入 `deploy/.env` 再启动适配器，无需先手动运行 `load_deploy_env.ps1`。  
2. **IntelliJ**：**Run → Edit Configurations → AdapterApplication**，在 **Environment variables** 里配置 **`DEEPSEEK_API_KEY`** 等，或安装 **EnvFile** 类插件指向 **`deploy/.env`**；点绿色运行即可，**不需要** PowerShell 脚本。  
3. **手动分步**：**`. .\scripts\load_deploy_env.ps1`** 再 **`mvn -pl adapter-service spring-boot:run`**。

**说明**：IntelliJ **HTTP Client** 自带的 `.env` 只用于请求文件里的 **`{{变量}}` 替换**，**不会**注入到运行的 Java 服务。

### 启动与冒烟

1. 前提：`deploy/sql/` 已初始化（含 **`V3__seed_model_providers_deepseek.sql`**），MySQL 可连。
2. 启动 **`adapter-service`**（**8102**）：须让进程能读到 **`DEEPSEEK_API_KEY`**（见上节）；**`MYSQL_*`** 与库一致（默认库 `token_charge`）。
3. 启动 **`gateway-service`**（**8080**）：可选 **`JWT_SECRET`**（与用户中心 JWT 一致）；路由默认 **`TOKENS_GATEWAY_ADAPTER_URI=http://127.0.0.1:8102`**、`TOKENS_GATEWAY_USER_URI=http://127.0.0.1:8101`。
4. 上游 **`model`**：DeepSeek 当前文档要求 **`deepseek-v4-flash`** 或 **`deepseek-v4-pro`**（不要使用无效的占位名）。
5. 冒烟：

```bash
curl -s http://localhost:8080/v1/models -H "Authorization: Bearer any-opaque-or-jwt"
```
