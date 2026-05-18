# TraceGatewayFilter

| 项 | 内容 |
|----|------|
| 类 | `com.tokenhub.gateway.infrastructure.web.TraceGatewayFilter` |
| Order | `Ordered.HIGHEST_PRECEDENCE`（最先执行） |
| 关联 | 全链路可观测；所有业务错误 JSON 携带 `traceId` |

---

## 1. 背景

分布式系统中，一次用户请求会穿过网关、适配器、计费等多个服务。若没有统一关联 ID，日志只能按时间戳「猜」是否同属一次调用。

业界惯例：接受客户端传入的 Trace / Request ID，若无则服务端生成，并在**响应头**与**日志 MDC**（本过滤器仅做头与 exchange 属性，MDC 需下游配合）中透传。

---

## 2. 作用

1. 读取请求头 `X-Trace-Id`；为空则生成 `UUID`。
2. 若客户端传入非空头，**长度不得超过 128**（超长返回 `400` / `I400002`）。
3. 写回**变异后的请求**（下游可见同一 ID）。
4. 存入 `exchange` 属性 `gateway.traceId`（`TRACE_ATTR`），供其他过滤器写错误 JSON。
5. 在响应提交前（`beforeCommit`）设置响应头 `X-Trace-Id`。

> **与扣费幂等（O-10）**：Chat **结算**幂等请使用 `X-Idempotency-Key`（网关合成 `X-Idempotency-Key-Composite`），见 [08-IdempotencyGatewayFilter.md](./08-IdempotencyGatewayFilter.md)。**平台侧**不重复调上游模型由 adapter Redis 响应缓存承担，见 [07-ChatIdempotencyResponseCache.md](../../../adapter-service/docs/components/07-ChatIdempotencyResponseCache.md)。`X-Trace-Id` 主要用于观测与 `request_orders.trace_id` 字段；仅在未传客户端幂等键时作为 `TRACE_ID_FALLBACK` 复合键的一部分。

---

## 3. 触发条件

**无条件**：对所有 HTTP 方法、所有路径执行（无短路）。

---

## 4. 实现要点

```25:54:gateway-service/src/main/java/com/tokenhub/gateway/infrastructure/web/TraceGatewayFilter.java
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String incoming = request.getHeaders().getFirst(TRACE_HEADER);
    String traceId;
    if (incoming == null || incoming.isBlank()) {
      traceId = UUID.randomUUID().toString();
    } else {
      traceId = incoming.trim();
      if (traceId.length() > TRACE_ID_MAX_LENGTH) {
        return GatewayJsonResponses.writeBusiness(
            exchange.getResponse(),
            HttpStatus.BAD_REQUEST.value(),
            traceId.substring(0, TRACE_ID_MAX_LENGTH),
            "I400002",
            "X-Trace-Id 长度不能超过 " + TRACE_ID_MAX_LENGTH
        );
      }
    }
    // ... 写请求头、TRACE_ATTR、响应 beforeCommit ...
    return chain.filter(exchange.mutate().request(mutated).build());
  }
```

常量：

- 头名：`X-Trace-Id`（`TRACE_HEADER`）
- 属性键：`gateway.traceId`（`TRACE_ATTR`）
- 最大长度：`TRACE_ID_MAX_LENGTH = 128`

**错误码**（超长时，请求**不会**进入后续过滤器与 adapter）：

| HTTP | code | 场景 |
|------|------|------|
| 400 | `I400002` | 客户端 `X-Trace-Id` 经 `trim` 后长度 &gt; 128 |

错误 JSON 中的 `traceId` 字段为截断后的前 128 字符（便于仍能在日志中检索部分客户端值）。

---

## 5. 优劣分析

| 优点 | 缺点 |
|------|------|
| 实现极简、零外部依赖 | 未对接 OpenTelemetry / W3C `traceparent` 标准 |
| 客户端可自带 ID 便于联调 | 仅校验最大长度，不校验 UUID 格式；与 OTel 未对齐 |
| 超长头直接 `400`，避免脏数据进下游 | 超长时错误体里的 traceId 为截断值，与原始头不完全一致 |
| `beforeCommit` 保证响应头一致 | 未自动注入 SLF4J MDC（需 Logback 配置或后续过滤器） |

---

## 6. 业界更好的方案

| 方案 | 说明 |
|------|------|
| **OpenTelemetry** | 标准 Trace/Span；Spring Boot 3 + Micrometer Tracing 自动传播 `traceparent` |
| **W3C Trace Context** | `traceparent` / `tracestate` 与云厂商、APM 互通 |
| **Spring Cloud Sleuth → Micrometer** | 与 Gateway 集成成熟，可在 Reactor 链自动传递 |
| **Envoy / Service Mesh** | 数据面自动生成并传播，应用无感 |

**建议演进**：保留 `X-Trace-Id` 作为对外兼容别名，内部逐步对齐 `traceparent`；在 `GatewayApplication` 或全局 `WebFilter` 中启用 Micrometer Observation。

---

## 7. 配置项

无独立 `application.yml` 开关；行为固定（最大长度见源码常量 `TRACE_ID_MAX_LENGTH`）。

与 O-10 相关的 Chat 幂等、adapter 缓存配置见 [08-IdempotencyGatewayFilter.md](./08-IdempotencyGatewayFilter.md) 与 adapter `tokenhub.adapter.idempotency-cache.*`。

---

## 8. 相关文档

- [08-IdempotencyGatewayFilter.md](./08-IdempotencyGatewayFilter.md)（Chat 扣费幂等，O-10）
- [adapter 07-ChatIdempotencyResponseCache.md](../../../adapter-service/docs/components/07-ChatIdempotencyResponseCache.md)（平台侧响应缓存）
- [09-错误响应与头约定.md](../09-错误响应与头约定.md)
- [00-网关总览.md](../00-网关总览.md)





## 9. 分布式下traceid的逻辑


结合 `TraceGatewayFilter` 的实现说明 TraceId 在分布式场景下的行为。

**简短结论：生成 TraceId 本身不受「分布式」牵制**——不依赖 Redis、注册中心或全局发号器；每个网关节点在本地用 `UUID.randomUUID()` 或透传客户端头即可。需要区分的是：**关联 ID 的语义**在分布式里怎么传递、重试时会不会变。

## 当前实现做了什么

```28:42:gateway-service/src/main/java/com/tokenhub/gateway/infrastructure/web/TraceGatewayFilter.java
    if (incoming == null || incoming.isBlank()) {
      traceId = UUID.randomUUID().toString();
    } else {
      traceId = incoming.trim();
      if (traceId.length() > TRACE_ID_MAX_LENGTH) {
        return GatewayJsonResponses.writeBusiness(..., "I400002", ...);
      }
    }
    ServerHttpRequest mutated = request.mutate().header(TRACE_HEADER, traceId).build();
    exchange.getAttributes().put(TRACE_ATTR, traceId);
```

逻辑：

| 情况 | TraceId 来源 |
|------|----------------|
| 请求**没有** `X-Trace-Id`（或为空） | 本机 `UUID.randomUUID()` |
| 请求**已有** `X-Trace-Id` 且长度 ≤ 128 | 原样 `trim()` 后使用，**不重新生成** |
| 请求**已有** `X-Trace-Id` 且长度 &gt; 128 | 网关 `400` / `I400002`，**不转发** |

之后把该值写进转发请求的 Header、`exchange` 属性，并在响应 `beforeCommit` 时写回响应头。

## 和「分布式」的关系

### 1. 多实例网关（水平扩展）

- 每个 Pod/进程**各自**调 JVM 的 UUID，**没有**跨节点协调或集中发号。
- 负载均衡把一次 HTTP 请求打到**某一个**实例，由该实例生成或透传 ID 即可，**不需要**实例间共享状态。
- UUID v4 碰撞概率极低，用作日志关联 ID 足够；这不是 Snowflake 那种「全局单调」场景。

因此：**多副本不会导致「生成失败」或「必须抢分布式锁」**，只意味着不同请求在不同机器上各生成各的 ID——这是预期行为。

### 2. 不受分布式影响的部分

- 不读 Redis / DB
- 不依赖机器时钟对齐（UUID 与时间戳发号无关）
- Reactor 下每次请求在 filter 里局部变量赋值，无共享可变状态

### 3. 分布式下仍要注意的行为（不是生成算法问题）

**（1）客户端是否自带 TraceId**

- 若 SDK/前端每次请求都带同一个 `X-Trace-Id`，多实例网关都会**透传**，全链路一致。
- 若不带，每次进网关可能得到**新 UUID**；重试若打到不同实例且仍不带头，会得到**另一个** TraceId。

**（2）与业务幂等的关系**

- **Chat 按量结算（O-10）**：优先使用客户端 `X-Idempotency-Key`（UUID v4），网关合成 `X-Idempotency-Key-Composite` 作为 `request_orders.idempotency_key`；未提供时回退 `userId:apiKeyId:traceId`（`TRACE_ID_FALLBACK`）。`IdempotencyGatewayFilter`（Order +12）读取本过滤器写入的 `gateway.traceId`（`TRACE_ATTR`）用于回退复合键与错误 JSON。
- **Chat 调上游（O-10 扩展）**：adapter 对同一 `X-Idempotency-Key-Composite` 缓存成功响应；重试命中缓存时**不调模型、不调 settle**（见 adapter 文档）。
- **`X-Trace-Id`**：仍写入订单 `trace_id` 列并用于日志；**不应**再单独依赖 traceId 防重试重复扣费或重复调模型。
- **预占 `reserve`（O-3）**：内部 API 仍以 `trace_id` 为幂等键（与 Trace 对齐）。

因此：

- **同一次 Chat 重试**应固定 `X-Idempotency-Key`；`X-Trace-Id` 可固定或每次新建（以复合幂等键为准；固定幂等键时换 traceId 仍安全）。
- 这与「网关是否分布式」无关，与「幂等键是否在重试间保持一致」有关。

**（3）下游 MVC 服务**

`user-center`、`billing` 等使用 `common-web` 的 `TraceBootstrapFilter`：同样「有头则用、无头则本机 UUID」。  
网关转发时已带上 `X-Trace-Id`，下游一般会**沿用同一 ID**；若绕过网关直连下游且不带头，下游会再生成一个，日志会对不齐。

**（4）没有分布式追踪标准**

当前是**单 ID 透传**，不是 W3C `traceparent` 的 Trace/Span 树；多服务并行调用时也不会自动拆子 Span。这是可观测性模型上的局限，不是 UUID 在分布式下「生成不了」。

## 总结表

| 问题 | 答案 |
|------|------|
| 多网关实例会不会冲突/要协调？ | 不需要；各实例独立 UUID，碰撞可忽略 |
| 要不要 Redis 发号？ | Trace 生成不需要；Chat 响应幂等缓存用 Redis（在 adapter） |
| 时钟漂移会影响 TraceId？ | 不会（UUID 不依赖时钟） |
| 分布式下 Trace 会断吗？ | 若客户端不传头、重试换新 ID、或绕过网关，可能断链 |
| 和 OpenTelemetry 比？ | 本实现更简单；OTel 才解决跨服务 Span 传播 |

若关心「多副本网关 + 客户端重试会不会重复扣费/重复调模型」，重点应放在：**是否固定 `X-Idempotency-Key`**（及 adapter 响应缓存），而不是网关本地 UUID 是否可靠。



## 10、如果traceid重试的时候换了会怎么样？

追踪 `traceId` 在扣费、预占、入账等路径中的用法，评估重试换新 ID 的影响。

**重试时若网关生成新的 TraceId，且未固定 `X-Idempotency-Key`，等价于一次新的业务请求**——扣费幂等键对不上，也无法命中 adapter 响应缓存，无法把重试当成「同一次调用」。下面按仓库里的真实链路说明影响（**2026-05 O-10 + 响应缓存**）。

## TraceId 在扣费里扮演什么（2026-05 起与 O-10 并存）

adapter 结算请求体始终带 **`traceId`**（来自 `X-Trace-Id`）。**扣费幂等键**为 `idempotencyKey`（网关注入的 `X-Idempotency-Key-Composite`）；若为空则 billing **回退** `traceId`。

```98:110:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/billing/BillingSettlementClient.java
    body.put("traceId", traceId.trim());
    // ...
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      body.put("idempotencyKey", idempotencyKey.trim());
    }
    if (idempotencySource != null && !idempotencySource.isBlank()) {
      body.put("idempotencySource", idempotencySource.trim());
    }
```

`BillingSettlementApplicationService.settle` 使用 **`effectiveIdempotencyKey`**（优先 `idempotencyKey`，否则 `traceId`）：

```71:76:billing-service/src/main/java/com/tokenhub/billing/application/BillingSettlementApplicationService.java
    RequestOrderPo done = requestOrderMapper.selectOne(
        new LambdaQueryWrapper<RequestOrderPo>()
            .eq(RequestOrderPo::getIdempotencyKey, effectiveIdempotencyKey)
    );
    if (done != null && "COMPLETED".equals(done.getBillingStatus())) {
      return;
    }
```

因此：**只有「同一条 `effectiveIdempotencyKey`」的重复结算请求，才会被当成幂等**（优先客户端 `X-Idempotency-Key` 合成的复合键）。

---

## 重试换新 ID 时会发生什么

典型路径：客户端/SDK **整包重试** `POST /v1/chat/completions`，且**不带** `X-Trace-Id` → 网关每次 `UUID.randomUUID()` → adapter 再调一次模型 → 再用**新 traceId** 调 `/internal/billing/settle`。

| 环节 | 幂等键 | 换新 TraceId 且**未**固定 `X-Idempotency-Key` |
|------|--------|-----------------------------------------------|
| **模型调用**（adapter → 供应商） | `X-Idempotency-Key-Composite`（Redis 响应缓存） | 复合键变 → **缓存未命中**，可能再调模型 |
| **按量扣费 settle** | `effectiveIdempotencyKey`（优先复合键，否则 traceId） | **会再扣一笔**（`TRACE_ID_FALLBACK` 时复合键含新 traceId） |
| **网关余额预检** | 否 | 每次重试再查余额 |
| **秒级限流 / 日配额** | 否 | 每次重试仍占配额 |
| **充值入账**（payment） | `orderNo` / `sourceRef` | 与 TraceId 无关 |
| **预占 reserve**（O-3） | `trace_id` | 新 ID = 新预占行 |

adapter Chat 路径（有复合幂等键且 Redis 可用时）由 **`IdempotentChatCompletionApplicationService`** 编排：

```27:31:adapter-service/src/main/java/com/tokenhub/adapter/presentation/OpenAiCompatibleController.java
  public JsonNode chatCompletions(@RequestBody JsonNode body, HttpServletRequest request) {
    return idempotentChatCompletionApplicationService.complete(body, request);
  }
```

- **缓存命中**：直接返回 JSON，不调上游、不调 `trySettle`。
- **缓存未命中**：`chat` → `trySettle`（2xx 后写 Redis，默认 TTL 24h）。

因此：**重试若被当成新 HTTP，且幂等键（或 trace 回退键）变了，扣费与模型调用都会按新单处理。**

---

## 几种具体场景

### 1. 第一次已成功扣费，客户端因超时/5xx 重试（最常见、最危险）

- **未固定 `X-Idempotency-Key`**：第一次 `traceId=A` → 模型 + settle → **已 debit**；重试 `traceId=B` → 模型可能再跑 → settle(B) → **再 debit**。表现：余额扣两次、两条 `request_orders`；TraceId 不同。
- **已固定 `X-Idempotency-Key`**：复合键不变 → 第二次 **Redis 缓存命中** → **不调模型、不重复 settle**（推荐生产行为）。

### 2. 第一次模型成功，但 settle 失败（`trySettle` 返回 false，不写响应缓存）

```116:121:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/billing/BillingSettlementClient.java
    try {
      var response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception ex) {
      log.warn("billing settle failed: {}", ex.toString());
      return false;
    }
```

- 若重试**未固定幂等键**且换新 TraceId：会再调模型 + 再 settle。
- 若重试**固定同一 `X-Idempotency-Key`**：第一次未写缓存 → 第二次仍会调模型，但 settle 可幂等/补扣（billing 侧 `COMPLETED` 则不再扣）。
- 若重试固定幂等键且第一次已成功 settle 并写缓存：第二次缓存命中，不再调模型。

### 3. 第一次在进入 adapter 前就失败（401/429/402 预检）

- 通常没有 usage、没有 settle → 换新 ID 重试一般 **不会多扣费**（但限流/日配额可能多计一次）。

### 4. 并发双发（两个请求、两个 TraceId）

- 两笔独立扣费，与「重试换新 ID」同类，**无互斥**（除非上 O-1 用户锁，也只缓解余额竞争，不合并幂等键）。

---

## 与「同一 TraceId / 同一幂等键重试」的对比

| | 固定 `X-Idempotency-Key`（推荐） | 仅同一 `X-Trace-Id`（无客户端幂等键） | 新 TraceId + 无客户端幂等键 |
|--|----------------------------------|--------------------------------------|------------------------------|
| 模型调用 | 缓存命中则**不调** | 每次可能调模型 | 每次可能调模型 |
| 已成功 `COMPLETED` | `settle` 跳过或幂等 return | 同 trace 回退键可幂等 | 新订单，**再扣款** |
| 预占 `reserve` | 与 Chat 复合键无关 | 同 traceId 返回已有行 | 新预占 |

同一 traceId 下，`DuplicateKeyException` 分支是 **直接 return**，不区分对方是否已 `COMPLETED`——这是并发下的细节风险，但和你问的「换新 ID」相比，**换新 ID 明确会绕过幂等**。

---

## 对你业务的实际建议

1. **客户端 / SDK 重试 Chat 时必须复用同一个 `X-Idempotency-Key`（UUID v4）**；可同时固定或更换 `X-Trace-Id`（扣费与模型幂等以复合键为准）。
2. **`X-Trace-Id`** 用于日志与排障；长度 ≤ 128，超长网关直接 `400` / `I400002`。
3. 未传 `X-Idempotency-Key` 时网关使用 `TRACE_ID_FALLBACK`（复合键含 traceId）——重试换新 traceId 仍有**重复扣费与重复调模型**风险。
4. **预检、限流**不用幂等键；换新 traceId 不能防止重复占配额。
5. 同 Key 不同 body：当前返回**首次**缓存响应（防薅平台）；新意图应使用新幂等键。

---

## 一句话总结

**重试换新 TraceId 且未固定 `X-Idempotency-Key` → 复合幂等键变化 → billing 会再扣费、adapter 会再调模型（缓存未命中）。**  
**固定 `X-Idempotency-Key` → 同一复合键 → settle 幂等 + Redis 响应缓存，用户侧不重复扣费、平台侧不重复调上游。**  
`X-Trace-Id` 负责观测与订单 `trace_id`；充值仍用 `orderNo`，与 Trace 无关。