# 在 IntelliJ IDEA 里本地启动（不推荐多开 CMD）

与 `scripts/dev-up.ps1` 配合：脚本只做 **Docker（可选）** + **`mvn install`**；**各 Spring Boot 进程在 IDEA 里 Run**。

## 1. 准备

```powershell
cd <仓库根目录>
.\scripts\dev-up.ps1
# 若需容器里起 MySQL/Redis：
.\scripts\dev-up.ps1 -Docker
```

## 2. 导入工程

用 IDEA **打开根目录 `pom.xml`**（或 **Open** 仓库根，识别为 Maven），等待索引与依赖下载完成。

## 3. 为每个服务生成运行配置

打开下表中的 **`*Application.java`**，对类或 `main` 左侧绿色三角选择 **Run**（首次会为该模块生成 Run Configuration）。

| 顺序 | 服务（Maven 模块 / 含义） | 入口类 | 端口 |
|------|---------------------------|--------|------|
| 1 | **user-center-service**（用户与认证） | `com.tokenhub.usercenter.UserCenterApplication` | 8101 |
| 2 | **billing-service**（计费账务） | `com.tokenhub.billing.BillingApplication` | 8103 |
| 3 | **adapter-service**（模型供应商适配） | `com.tokenhub.adapter.AdapterApplication` | 8102 |
| 4 | **payment-service**（支付与入账） | `com.tokenhub.payment.PaymentApplication` | 8104 |
| 5 | **ops-console**（运营控制面 API） | `com.tokenhub.ops.OpsConsoleApplication` | 8105 |
| 6 | **gateway-service**（统一网关） | `com.tokenhub.gateway.GatewayApplication` | 8080 |

**gateway-service** 依赖 Redis 与其它后端已监听，**建议最后启动 gateway-service**。

## 4. 环境变量（与 `deploy/.env` 一致）

在 **Run -> Edit Configurations** 中，对上述 6 个配置勾选 **Modify options -> Environment variables**，填入与本地数据库/密钥一致的变量。

可在 PowerShell 中生成一行便于粘贴的格式：

```powershell
.\scripts\print-idea-env.ps1
```

将输出整行粘贴到 IDEA 的 **Environment variables**；或安装 **EnvFile** 插件直接指向 `deploy/.env`。

**至少保证**：`MYSQL_PASSWORD`（或与 `MYSQL_ROOT_PASSWORD` 一致）、`JWT_SECRET`（与用户中心一致）、`BILLING_INTERNAL_TOKEN`（网关/计费/适配器/支付一致）。未设置时开发默认多为 `dev-internal-token`。

## 5. Compound「一键」启动（可选）

### 5.1 操作步骤（IDEA 界面）

前提：第 3 节里每个服务都已经 **Run 过一次**，左侧运行配置下拉框里能看到 6 个独立配置（名称可能是 `UserCenterApplication`、`BillingApplication` 等）。

1. 菜单 **Run → Edit Configurations…**（或运行配置下拉 → **Edit Configurations…**）。
2. 左上角 **+ → Compound**。
3. **Name** 填例如：`TokenHub - all services`。
4. 在 **compound** 的中间区域，点右侧 **+**（或 **Add**），在列表里 **依次勾选** 已存在的 6 个 Spring Boot 配置，使你希望的顺序出现在列表中（从上到下）：
   - 建议列表顺序与上表一致：**user-center-service → billing-service → adapter-service → payment-service → ops-console → gateway-service**（**gateway-service 放最后**；括号内为对应端口：8101 → 8103 → 8102 → 8104 → 8105 → 8080）。
5. 若顺序不对：在列表中 **拖拽** 行调整上下顺序。
6. **Apply → OK**。
7. 运行配置下拉里选中 **`TokenHub - all services`**，点绿色 **Run**（或 **Debug**）。

说明：**环境变量不会自动合并**——请保证第 4 节里的变量已分别配在这 6 个子配置上（或父级用 EnvFile）；Compound 本身一般不再单独配置一份 env。

### 5.2 「顺序」在 IDEA 里实际会怎样

在较新的 IntelliJ 中，**Compound 里多个子配置通常是「同时启动」的**（并行），**不是严格等前一个完全起来再启动下一个**。列表里的上下顺序 **主要便于你整理思路**，不能保证严格的先后启动。

本地一般仍可工作；若 **gateway-service（8080）** 偶尔因 Redis / 下游未就绪报错，可：**单独再 Run 一次 gateway-service**，或第一次不用 Compound、按上表 **依次 Run 六个服务（间隔几秒）**。

若必须「先起完再起网关」，只能用 **Before launch** 等折中（例如在 **gateway-service** 的配置上设置 **Before launch → Run another configuration** 先跑其它服务），较繁琐；多数人用并行 Compound 或对 **gateway-service** **补跑一次**即可。

## 6. 前端

```bash
cd console-web
npm install
npm run dev
```

浏览器：<http://localhost:5173>，API 经 Vite 代理到 **gateway-service**（**8080**）（见 `console-web/vite.config.ts`）。

## 7. 仅重编译不写死窗口时

```powershell
.\scripts\dev-up.ps1 -SkipInstall
```

仅打印说明；若从未 `install` 过，不要用 `-SkipInstall`。
