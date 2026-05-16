# ProviderAdapters（DeepSeek + 智谱）

| 项 | 内容 |
|----|------|
| 接口 | `com.tokenhub.adapter.domain.provider.ProviderAdapter` |
| DeepSeek | `com.tokenhub.adapter.infrastructure.deepseek.DeepSeekProviderAdapter` |
| 智谱 | `com.tokenhub.adapter.infrastructure.zhipu.ZhipuProviderAdapter` |
| HTTP | 共享 Bean `adapterRestTemplate`（[08-路由与配置.md](../08-路由与配置.md)） |
| 注册表 | `ModelProviderRegistry` → MySQL `model_providers` |

---

## 1. 背景

各模型厂商提供 **OpenAI 兼容** 或近似的 HTTP API，但 **URL 路径、默认 model、错误 JSON** 仍有差异。两个 `@Component` 适配器将平台统一的 `JsonNode` 请求/响应与具体上游对接；**不**作为 `@Primary` 注入应用层，由 `FailoverRoutingAdapter` 编排调用。

---

## 2. 接口契约

```5:15:adapter-service/src/main/java/com/tokenhub/adapter/domain/provider/ProviderAdapter.java
public interface ProviderAdapter {

  String providerCode();

  JsonNode chat(JsonNode openAiRequestBody);

  JsonNode listModels();

  default JsonNode embeddings(JsonNode openAiRequestBody) {
    throw new UnsupportedOperationException("embeddings");
  }
}
```

---

## 3. DeepSeekProviderAdapter

| 项 | 内容 |
|----|------|
| `providerCode` 配置 | `tokenhub.adapter.provider-code`，默认 `deepseek` |
| API Key | `DEEPSEEK_API_KEY` / `tokenhub.adapter.deepseek-api-key` |
| Base URL | DB `model_providers.base_url`（enabled）或 `deepseek-base-url-fallback`，默认 `https://api.deepseek.com` |
| Chat URL | `{base}/v1/chat/completions` |
| 目录 model | `tokenhub.adapter.default-chat-model`，默认 `deepseek-v4-flash` |

**调用方式**：

- `RestTemplate.exchange(POST)`，`Authorization: Bearer {apiKey}`
- 4xx → `BusinessException(BAD_REQUEST)`；5xx → `INTERNAL`
- 解析上游 `error.message` 写入业务异常文案

**缺 Key**：抛出 `INTERNAL`，提示配置 `DEEPSEEK_API_KEY`。

```64:68:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/deepseek/DeepSeekProviderAdapter.java
  public JsonNode chat(JsonNode openAiRequestBody) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new BusinessException(ErrorCode.INTERNAL, "缺少 DeepSeek 密钥：请配置环境变量 DEEPSEEK_API_KEY（或 tokenhub.adapter.deepseek-api-key）");
    }
```

**listModels**：返回单条静态目录（不调用上游 models API），`owned_by` = `providerCode`。

---

## 4. ZhipuProviderAdapter

| 项 | 内容 |
|----|------|
| `providerCode` 配置 | `tokenhub.adapter.zhipu-provider-code`，默认 `zhipu` |
| API Key | `ZHIPU_API_KEY` / `tokenhub.adapter.zhipu-api-key` |
| Base URL | DB 或 fallback，默认 `https://open.bigmodel.cn/api/paas/v4` |
| Chat URL | `{base}/chat/completions`（**无** `/v1` 前缀） |
| 目录 model | `zhipu-default-chat-model`，默认 `glm-4-flash` |

**`isConfigured()`**：Key 非空才参与加权首跳与 failover secondary。

```61:63:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/zhipu/ZhipuProviderAdapter.java
  public boolean isConfigured() {
    return apiKey != null && !apiKey.isBlank();
  }
```

错误映射与 DeepSeek 对称（4xx/5xx → `BusinessException`）。

---

## 5. 对比速查

| 维度 | DeepSeek | 智谱 |
|------|----------|------|
| Chat 路径 | `/v1/chat/completions` | `/chat/completions` |
| 默认 catalog model | `deepseek-v4-flash` | `glm-4-flash` |
| 熔断 | Resilience4j `deepseek` | 无 |
| 未配置 Key | 运行期报错（主路径常用） | 跳过加权/作 secondary |
| 官方文档 | api-docs.deepseek.com | open.bigmodel.cn OpenAI 兼容 v4 |

---

## 6. ModelProviderRegistry

```18:26:adapter-service/src/main/java/com/tokenhub/adapter/infrastructure/provider/ModelProviderRegistry.java
  public Optional<ModelProviderPo> findEnabled(String code) {
    return Optional.ofNullable(
        mapper.selectOne(
            new LambdaQueryWrapper<ModelProviderPo>()
                .eq(ModelProviderPo::getCode, code)
                .eq(ModelProviderPo::getEnabled, true)
        )
    );
  }
```

DB 不可用时 **warn** 并回退到 yml 中的 `*-base-url-fallback`，不阻断启动。

---

## 7. 配置项汇总

| 配置键 | 环境变量 | 默认 |
|--------|----------|------|
| `tokenhub.adapter.provider-code` | — | `deepseek` |
| `tokenhub.adapter.deepseek-api-key` | `DEEPSEEK_API_KEY` | 空 |
| `tokenhub.adapter.deepseek-base-url-fallback` | `DEEPSEEK_BASE_URL_FALLBACK` | `https://api.deepseek.com` |
| `tokenhub.adapter.default-chat-model` | — | `deepseek-v4-flash` |
| `tokenhub.adapter.zhipu-provider-code` | — | `zhipu` |
| `tokenhub.adapter.zhipu-api-key` | `ZHIPU_API_KEY` | 空 |
| `tokenhub.adapter.zhipu-base-url-fallback` | `ZHIPU_BASE_URL_FALLBACK` | `https://open.bigmodel.cn/api/paas/v4` |
| `tokenhub.adapter.zhipu-default-chat-model` | — | `glm-4-flash` |

---

## 8. 相关文档

- [02-FailoverRoutingAdapter.md](./02-FailoverRoutingAdapter.md)
- [03-WeightedRoutingPolicy.md](./03-WeightedRoutingPolicy.md)
- [08-路由与配置.md](../08-路由与配置.md)
