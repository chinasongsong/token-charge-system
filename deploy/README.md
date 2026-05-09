# 部署说明（Docker）

本目录存放与本仓库版本同行的 **可提交** 部署资产：Compose、环境变量示例、后续各服务的镜像构建入口引用等。

## 约定

- **随仓维护**：Compose、`Dockerfile` 引用路径、初始化 SQL（建议放在 `deploy/sql/` 或各服务模块下）、OpenAPI 与网关静态配置模板等均保留在仓库中，便于评审与回滚。
- **密钥不入库**：复制 `env.example` 为本地 `.env`（勿提交），在生产环境用密钥管理或编排注入同名变量。
- **演进**：Java 多模块与各业务镜像落地后，在本文件中补充构建命令与端口映射；当前 Compose 仅包含常用基础设施骨架（MySQL、Redis），应用服务待接入。

## 快速开始（基础设施）

在项目根目录执行：

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d
```

若尚未创建 `deploy/.env`，可先：`copy deploy\env.example deploy\.env`（Windows）或 `cp deploy/env.example deploy/.env`（Unix），再按需修改口令。
