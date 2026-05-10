package com.tokenhub.adapter.infrastructure.zhipu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tokenhub.adapter.domain.provider.ProviderAdapter;
import com.tokenhub.adapter.infrastructure.persistence.ModelProviderPo;
import com.tokenhub.adapter.infrastructure.provider.ModelProviderRegistry;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * 智谱 OpenAI 兼容：{@code POST {base}/chat/completions}，base 形如 {@code https://open.bigmodel.cn/api/paas/v4}。
 */
@Component
public class ZhipuProviderAdapter implements ProviderAdapter {

  private static final Logger log = LoggerFactory.getLogger(ZhipuProviderAdapter.class);

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final ModelProviderRegistry registry;
  private final String providerCode;
  private final String apiKey;
  private final String fallbackBaseUrl;
  private final String catalogModelId;

  public ZhipuProviderAdapter(
      RestTemplate adapterRestTemplate,
      ObjectMapper objectMapper,
      ModelProviderRegistry registry,
      @Value("${tokenhub.adapter.zhipu-provider-code:zhipu}") String providerCode,
      @Value("${tokenhub.adapter.zhipu-api-key:}") String apiKey,
      @Value("${tokenhub.adapter.zhipu-base-url-fallback:https://open.bigmodel.cn/api/paas/v4}") String fallbackBaseUrl,
      @Value("${tokenhub.adapter.zhipu-default-chat-model:glm-4-flash}") String catalogModelId
  ) {
    this.restTemplate = adapterRestTemplate;
    this.objectMapper = objectMapper;
    this.registry = registry;
    this.providerCode = providerCode;
    this.apiKey = apiKey;
    this.fallbackBaseUrl = trimTrailingSlash(fallbackBaseUrl);
    this.catalogModelId = catalogModelId;
  }

  public boolean isConfigured() {
    return apiKey != null && !apiKey.isBlank();
  }

  @Override
  public String providerCode() {
    return providerCode;
  }

  @Override
  public JsonNode chat(JsonNode openAiRequestBody) {
    if (!isConfigured()) {
      throw new BusinessException(ErrorCode.INTERNAL, "缺少智谱密钥：请配置环境变量 ZHIPU_API_KEY（或 tokenhub.adapter.zhipu-api-key）");
    }
    String base = resolveBaseUrl();
    String url = base + "/chat/completions";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);
    try {
      String payload = objectMapper.writeValueAsString(openAiRequestBody);
      HttpEntity<String> entity = new HttpEntity<>(payload, headers);
      ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
      if (resp.getBody() == null || resp.getBody().isBlank()) {
        throw new BusinessException(ErrorCode.INTERNAL, "上游返回空响应");
      }
      return objectMapper.readTree(resp.getBody());
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      throw mapUpstream(ex);
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL, "上游调用失败: " + ex.getMessage(), ex);
    }
  }

  @Override
  public JsonNode listModels() {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("object", "list");
    ArrayNode data = objectMapper.createArrayNode();
    ObjectNode model = objectMapper.createObjectNode();
    model.put("id", catalogModelId);
    model.put("object", "model");
    model.put("owned_by", providerCode);
    data.add(model);
    root.set("data", data);
    return root;
  }

  private String resolveBaseUrl() {
    try {
      return registry.findEnabled(providerCode)
          .map(ModelProviderPo::getBaseUrl)
          .filter(s -> s != null && !s.isBlank())
          .map(ZhipuProviderAdapter::trimTrailingSlash)
          .orElse(fallbackBaseUrl);
    } catch (Exception ex) {
      log.warn("读取 model_providers 失败，使用 fallback base_url: {}", ex.toString());
      return fallbackBaseUrl;
    }
  }

  private static String trimTrailingSlash(String base) {
    if (base == null || base.isBlank()) {
      return "";
    }
    String s = base.trim();
    while (s.endsWith("/")) {
      s = s.substring(0, s.length() - 1);
    }
    return s;
  }

  private BusinessException mapUpstream(RestClientResponseException ex) {
    String raw = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
    String detail = raw;
    try {
      JsonNode tree = objectMapper.readTree(raw);
      if (tree.has("error") && tree.get("error").has("message")) {
        detail = tree.get("error").get("message").asText();
      }
    } catch (Exception ignored) {
      // keep raw
    }
    ErrorCode code = ex.getStatusCode().is4xxClientError() ? ErrorCode.BAD_REQUEST : ErrorCode.INTERNAL;
    return new BusinessException(code, "上游模型错误: " + detail);
  }
}
