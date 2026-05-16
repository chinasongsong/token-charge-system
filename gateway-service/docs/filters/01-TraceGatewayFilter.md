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
2. 写回**变异后的请求**（下游可见同一 ID）。
3. 存入 `exchange` 属性 `gateway.traceId`（`TRACE_ATTR`），供其他过滤器写错误 JSON。
4. 在响应提交前（`beforeCommit`）设置响应头 `X-Trace-Id`。

---

## 3. 触发条件

**无条件**：对所有 HTTP 方法、所有路径执行（无短路）。

---

## 4. 实现要点

```21:35:gateway-service/src/main/java/com/tokenhub/gateway/infrastructure/web/TraceGatewayFilter.java
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String incoming = request.getHeaders().getFirst(TRACE_HEADER);
    String traceId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming.trim();

    ServerHttpRequest mutated = request.mutate().header(TRACE_HEADER, traceId).build();
    exchange.getAttributes().put(TRACE_ATTR, traceId);

    ServerHttpResponse response = exchange.getResponse();
    response.beforeCommit(() -> {
      response.getHeaders().set(TRACE_HEADER, traceId);
      return Mono.empty();
    });

    return chain.filter(exchange.mutate().request(mutated).build());
  }
```

常量：

- 头名：`X-Trace-Id`（`TRACE_HEADER`）
- 属性键：`gateway.traceId`（`TRACE_ATTR`）

---

## 5. 优劣分析

| 优点 | 缺点 |
|------|------|
| 实现极简、零外部依赖 | 未对接 OpenTelemetry / W3C `traceparent` 标准 |
| 客户端可自带 ID 便于联调 | 未校验 ID 格式，恶意超长头可能影响日志 |
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

无独立配置；行为固定。

---

## 8. 相关文档

- [09-错误响应与头约定.md](../09-错误响应与头约定.md)
- [00-网关总览.md](../00-网关总览.md)





## 9. 分布式下traceid的逻辑


结合 `TraceGatewayFilter` 的实现说明 TraceId 在分布式场景下的行为。

**简短结论：生成 TraceId 本身不受「分布式」牵制**——不依赖 Redis、注册中心或全局发号器；每个网关节点在本地用 `UUID.randomUUID()` 或透传客户端头即可。需要区分的是：**关联 ID 的语义**在分布式里怎么传递、重试时会不会变。

## 当前实现做了什么

```21:27:gateway-service/src/main/java/com/tokenhub/gateway/infrastructure/web/TraceGatewayFilter.java
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String incoming = request.getHeaders().getFirst(TRACE_HEADER);
    String traceId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming.trim();

    ServerHttpRequest mutated = request.mutate().header(TRACE_HEADER, traceId).build();
    exchange.getAttributes().put(TRACE_ATTR, traceId);
```

逻辑只有两种：

| 情况 | TraceId 来源 |
|------|----------------|
| 请求**没有** `X-Trace-Id`（或为空） | 本机 `UUID.randomUUID()` |
| 请求**已有** `X-Trace-Id` | 原样 `trim()` 后使用，**不重新生成** |

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

本仓库里 `traceId` 还用于 billing 结算、预占等的**幂等键**（例如 `SettlementCommand.traceId`、`BalanceReservation`）。  
因此：

- **同一次业务**应在客户端或首跳固定 `X-Trace-Id`，重试时继续带上，避免被当成新请求。
- 这与「网关是否分布式」无关，与「ID 是否在重试间保持一致」有关。

**（3）下游 MVC 服务**

`user-center`、`billing` 等使用 `common-web` 的 `TraceBootstrapFilter`：同样「有头则用、无头则本机 UUID」。  
网关转发时已带上 `X-Trace-Id`，下游一般会**沿用同一 ID**；若绕过网关直连下游且不带头，下游会再生成一个，日志会对不齐。

**（4）没有分布式追踪标准**

当前是**单 ID 透传**，不是 W3C `traceparent` 的 Trace/Span 树；多服务并行调用时也不会自动拆子 Span。这是可观测性模型上的局限，不是 UUID 在分布式下「生成不了」。

## 总结表

| 问题 | 答案 |
|------|------|
| 多网关实例会不会冲突/要协调？ | 不需要；各实例独立 UUID，碰撞可忽略 |
| 要不要 Redis 发号？ | 当前实现不需要 |
| 时钟漂移会影响 TraceId？ | 不会（UUID 不依赖时钟） |
| 分布式下 Trace 会断吗？ | 若客户端不传头、重试换新 ID、或绕过网关，可能断链 |
| 和 OpenTelemetry 比？ | 本实现更简单；OTel 才解决跨服务 Span 传播 |

若你关心的是「多副本网关 + 客户端重试，会不会重复扣费」，重点应放在：**重试是否复用同一个 `X-Trace-Id`**（以及 billing 幂等），而不是网关本地 UUID 在分布式下是否可靠。需要的话可以再结合一次 `POST /v1/chat/completions` 的完整头传递路径说明。



## 10、如果traceid重试的时候换了会怎么样？
追踪 `traceId` 在扣费、预占、入账等路径中的用法，评估重试换新 ID 的影响。


[11 tools called]

**重试时若网关生成新的 TraceId，等价于一次新的业务请求**——扣费幂等键对不上，无法把重试当成「同一次调用」。下面按你仓库里的真实链路说明影响。

## TraceId 在扣费里扮演什么

在本项目中，`X-Trace-Id` 进入 adapter 后，会作为 **结算幂等键** 传给 billing：

```90:90:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/billing/BillingSettlementClient.java
    body.put("traceId", traceId);
```

`BillingSettlementApplicationService.settle` 用 **`traceId` = `request_orders.idempotency_key`**：

```60:82:billing-service/src/main/java/com/tokenhub/billing/application/BillingSettlementApplicationService.java
    RequestOrderPo done = requestOrderMapper.selectOne(
        new LambdaQueryWrapper<RequestOrderPo>()
            .eq(RequestOrderPo::getIdempotencyKey, cmd.traceId())
    );
    if (done != null && "COMPLETED".equals(done.getBillingStatus())) {
      return;  // 已成功结算过，直接返回，不再扣款
    }
    ...
    pending.setIdempotencyKey(cmd.traceId());
    try {
      requestOrderMapper.insert(pending);
    } catch (DuplicateKeyException ex) {
      return;  // 已有同 traceId 的订单行，直接返回（不再 debit）
    }
    ...
    accountBalanceApplicationService.debit(cmd.userId(), amount);
```

因此：**只有「同一条 `traceId`」的重复结算请求，才会被当成幂等**。

---

## 重试换新 ID 时会发生什么

典型路径：客户端/SDK **整包重试** `POST /v1/chat/completions`，且**不带** `X-Trace-Id` → 网关每次 `UUID.randomUUID()` → adapter 再调一次模型 → 再用**新 traceId** 调 `/internal/billing/settle`。

| 环节 | 是否用 traceId 幂等 | 换新 ID 的影响 |
|------|---------------------|----------------|
| **模型调用**（adapter → 供应商） | 否 | 可能再调一次模型，供应商侧多一笔用量 |
| **按量扣费 settle** | **是**（`idempotency_key = traceId`） | **会再扣一笔**，与第一次互不相认 |
| **网关余额预检** | 否（只传 `userId`） | 每次重试再查一次余额，不防重复扣费 |
| **秒级限流 / 日配额** | 否（按 apiKey/user + 时间桶） | 每次重试仍占配额 |
| **充值入账**（payment） | 否（用 `orderNo` 作 `sourceRef`） | 与 TraceId 无关 |
| **预占 reserve**（O-3，内部 API） | 是（`balance_reservation.trace_id`） | 新 ID = 新预占行；网关侧尚未接预占过滤器时，主要影响未来 M2 |

adapter 里是 **「一次 HTTP = 一次 chat + 一次 trySettle」**：

```27:31:adapter-service/src/main/java/com/tokenhub/adapter/presentation/OpenAiCompatibleController.java
  public JsonNode chatCompletions(@RequestBody JsonNode body, HttpServletRequest request) {
    JsonNode response = chatCompletionApplicationService.chat(body);
    billingSettlementClient.trySettle(request, body, response);
    return response;
  }
```

所以：**重试若被当成新 HTTP，且 TraceId 变了，扣费逻辑会按新单处理，不会合并到第一次。**

---

## 几种具体场景

### 1. 第一次已成功扣费，客户端因超时/5xx 重试（最常见、最危险）

- 第一次：`traceId=A`，模型成功 → settle(A) → **已 debit**。
- 重试：`traceId=B` → 模型可能再跑一遍 → settle(B) → **再 debit 一次**。

表现：**余额扣两次、两条 `request_orders`、用量记两笔**；日志里 TraceId 不同，难以一眼看出是「同一次用户意图」。

### 2. 第一次模型成功，但 settle 失败（adapter 只打 warn，不抛给客户端）

```101:105:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/billing/BillingSettlementClient.java
    try {
      restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
    } catch (Exception ex) {
      log.warn("billing settle failed: {}", ex.toString());
    }
```

- 若重试仍用 **新 TraceId**：会再调模型 + 再 settle → 可能 **供应商扣两次、平台只扣一次或两次**（取决于第一次 settle 是否落库）。
- 若重试 **沿用同一 TraceId**：settle 可正常幂等/补扣，相对安全。

### 3. 第一次在进入 adapter 前就失败（401/429/402 预检）

- 通常没有 usage、没有 settle → 换新 ID 重试一般 **不会多扣费**（但限流/日配额可能多计一次）。

### 4. 并发双发（两个请求、两个 TraceId）

- 两笔独立扣费，与「重试换新 ID」同类，**无互斥**（除非上 O-1 用户锁，也只缓解余额竞争，不合并幂等键）。

---

## 与「同一 TraceId 重试」的对比

| | 同一 `X-Trace-Id` | 新 TraceId |
|--|-------------------|------------|
| 已成功 `COMPLETED` | `settle` 直接 return，**不扣款** | 新订单，**再扣款** |
| 并发两次 settle | 一方 `DuplicateKeyException` 后 return（实现较粗，见下） | 两笔都可能在扣 |
| 预占 `reserve` | 同 traceId 返回已有行 | 新预占（多占可用额度） |

同一 traceId 下，`DuplicateKeyException` 分支是 **直接 return**，不区分对方是否已 `COMPLETED`——这是并发下的细节风险，但和你问的「换新 ID」相比，**换新 ID 明确会绕过幂等**。

---

## 对你业务的实际建议（不改代码层面的原则）

1. **客户端 / SDK 重试时必须复用同一个 `X-Trace-Id`**（或业务层自己生成 Idempotency-Key 并映射到头里），不要依赖网关每次新生成。
2. 把 TraceId 同时用于：**日志关联 + 结算幂等**；若需要与 OpenAI 的 request id 对齐，可在客户端生成 UUID 写入 `X-Trace-Id`。
3. 长期可考虑：**幂等键与 Trace 解耦**（例如 `Idempotency-Key` 由客户端按「一次用户操作」固定，Trace 仅观测）；或 adapter 在 body 里支持 OpenAI 式字段（若未来有）。
4. 注意：**预检、限流不用 traceId**，换新 ID 不能防止「重复请求多扣配额」，只能解决「结算幂等」这一层。

---

## 一句话总结

**重试导致新 TraceId → billing 的 `settle` 会认为是另一笔请求 → 在每次重试都完整走完「模型 + 有 usage 的响应」时，存在重复扣费风险。**  
幂等只保护「相同 traceId 的重复结算」，不保护「同一次用户操作、不同 traceId 的多次 HTTP」。充值路径用 `orderNo`，不受网关 TraceId 影响。