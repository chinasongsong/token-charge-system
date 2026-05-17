package com.tokenhub.adapter.infrastructure.billing;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 网关注入用户/幂等/trace 头后，将上游 usage 记账到 billing。
 */
@Component
public class BillingSettlementClient {

  private static final Logger log = LoggerFactory.getLogger(BillingSettlementClient.class);

  public static final String HEADER_USER_ID = "X-User-Id";
  public static final String HEADER_API_KEY_ID = "X-Api-Key-Id";
  public static final String HEADER_TRACE_ID = "X-Trace-Id";
  public static final String HEADER_IDEMPOTENCY_COMPOSITE = "X-Idempotency-Key-Composite";
  public static final String HEADER_IDEMPOTENCY_SOURCE = "X-Idempotency-Source";

  private final RestTemplate restTemplate;

  @Value("${tokenhub.billing.settlement-enabled:true}")
  private boolean settlementEnabled;

  @Value("${tokenhub.billing.base-url:http://127.0.0.1:8103}")
  private String billingBaseUrl;

  @Value("${BILLING_INTERNAL_TOKEN:dev-internal-token}")
  private String internalToken;

  @Value("${tokenhub.adapter.provider-code:deepseek}")
  private String providerCode;

  public BillingSettlementClient(@Qualifier("adapterRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public void trySettle(HttpServletRequest request, JsonNode chatRequest, JsonNode chatResponse) {
    if (!settlementEnabled || chatResponse == null || !chatResponse.isObject()) {
      return;
    }
    String userIdHeader = request.getHeader(HEADER_USER_ID);
    if (userIdHeader == null || userIdHeader.isBlank()) {
      return;
    }
    String traceId = request.getHeader(HEADER_TRACE_ID);
    if (traceId == null || traceId.isBlank()) {
      return;
    }
    long userId;
    try {
      userId = Long.parseLong(userIdHeader.trim());
    } catch (NumberFormatException ex) {
      return;
    }
    Long apiKeyId = null;
    String ak = request.getHeader(HEADER_API_KEY_ID);
    if (ak != null && !ak.isBlank()) {
      try {
        apiKeyId = Long.parseLong(ak.trim());
      } catch (NumberFormatException ignored) {
        return;
      }
    }

    long inputTokens = 0L;
    long outputTokens = 0L;
    if (chatResponse.has("usage") && chatResponse.get("usage").isObject()) {
      JsonNode u = chatResponse.get("usage");
      inputTokens = u.path("prompt_tokens").asLong(0);
      outputTokens = u.path("completion_tokens").asLong(0);
    }
    if (inputTokens == 0 && outputTokens == 0) {
      return;
    }

    String modelName = chatRequest.path("model").asText(null);
    if (modelName == null || modelName.isBlank()) {
      modelName = "deepseek-v4-flash";
    }

    String idempotencyKey = request.getHeader(HEADER_IDEMPOTENCY_COMPOSITE);
    String idempotencySource = request.getHeader(HEADER_IDEMPOTENCY_SOURCE);

    String base = trimTrailingSlash(billingBaseUrl);
    String url = base + "/internal/billing/settle";
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("traceId", traceId.trim());
    body.put("userId", userId);
    body.put("apiKeyId", apiKeyId);
    body.put("providerCode", resolveProviderForModel(modelName));
    body.put("modelName", modelName);
    body.put("inputTokens", inputTokens);
    body.put("outputTokens", outputTokens);
    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
      body.put("idempotencyKey", idempotencyKey.trim());
    }
    if (idempotencySource != null && !idempotencySource.isBlank()) {
      body.put("idempotencySource", idempotencySource.trim());
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Internal-Token", internalToken);
    try {
      restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
    } catch (Exception ex) {
      log.warn("billing settle failed: {}", ex.toString());
    }
  }

  private String resolveProviderForModel(String modelName) {
    if (modelName == null || modelName.isBlank()) {
      return providerCode;
    }
    String m = modelName.trim().toLowerCase();
    if (m.startsWith("glm") || m.contains("zhipu")) {
      return "zhipu";
    }
    return providerCode;
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
}
