# 部署说明（Docker）

本目录存放与本仓库版本同行的 **可提交** 部署资产：Compose、环境变量示例、后续各服务的镜像构建入口引用等。

## 约定

- **随仓维护**：Compose、`Dockerfile` 引用路径、初始化 SQL（建议放在 `deploy/sql/` 或各服务模块下）、OpenAPI 与网关静态配置模板等均保留在仓库中，便于评审与回滚。
- **密钥不入库**：复制 `env.example` 为本地 `.env`（勿提交），在生产环境用密钥管理或编排注入同名变量。
- **演进**：`docker-compose.yml` 已挂载 `deploy/sql/` 到 MySQL 初始化目录（首次启动会执行目录下 `V1__*.sql`、`V2__*.sql` 等）；RabbitMQ / Prometheus / Grafana 与各业务服务镜像仍为**注释占位**，按阶段启用。
- **Java 镜像构建**：在各服务目录下提供了多阶段 `Dockerfile`，构建上下文需为**仓库根目录**（参见各文件内注释）。根工程要求 **JDK 21**（`maven-enforcer-plugin`）。

## 快速开始（基础设施）

在项目根目录执行：

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d
```

若尚未创建 `deploy/.env`，可先：`copy deploy\env.example deploy\.env`（Windows）或 `cp deploy/env.example deploy/.env`（Unix），再按需修改口令。

## user-center-service（P1 起）

1. 确保 MySQL 已按 `deploy/sql/` 初始化且可连（默认库名 `token_charge`）。
2. 在仓库根执行：`mvn -pl user-center-service spring-boot:run`（需 JDK 21），并设置环境变量 `JWT_SECRET`（≥32 字符）、`MYSQL_JDBC_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD`（或沿用 `application.yml` 默认值仅本地）。
3. 冒烟示例（PowerShell，将 JSON 中的邮箱密码替换为你的值）：

```bash
curl -s -X POST http://localhost:8101/user/register -H "Content-Type: application/json" -d "{\"email\":\"you@example.com\",\"password\":\"password12\",\"displayName\":\"me\"}"
curl -s -X POST http://localhost:8101/user/login -H "Content-Type: application/json" -d "{\"email\":\"you@example.com\",\"password\":\"password12\"}"
curl -s http://localhost:8101/user/me -H "Authorization: Bearer <上一步返回的 accessToken>"
```
