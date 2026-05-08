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

## 架构守护

- 机械约束：`scripts/check_boundaries.py`
- 基线文件：`tests/architecture/known_violations.json`
- CI 执行：`.github/workflows/ci.yml`
- 漂移扫描：`scripts/gc_scan.py` + `.github/workflows/gc.yml`
