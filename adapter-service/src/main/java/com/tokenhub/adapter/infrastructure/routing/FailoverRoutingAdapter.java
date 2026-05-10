package com.tokenhub.adapter.infrastructure.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tokenhub.adapter.domain.provider.ProviderAdapter;
import com.tokenhub.adapter.domain.routing.ProviderRoute;
import com.tokenhub.adapter.domain.routing.RoutingPolicy;
import com.tokenhub.adapter.infrastructure.deepseek.DeepSeekProviderAdapter;
import com.tokenhub.adapter.infrastructure.risk.RiskEventRecorder;
import com.tokenhub.adapter.infrastructure.zhipu.ZhipuProviderAdapter;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.http.HttpStatusCode;

/**
 * Weighted first hop ({@link RoutingPolicy}) + DeepSeek 走熔断器；可恢复失败时切换对端并记录 {@code provider_failover}。
 */
public class FailoverRoutingAdapter implements ProviderAdapter {

  private final DeepSeekProviderAdapter deepSeekProviderAdapter;
  private final ZhipuProviderAdapter zhipuProviderAdapter;
  private final RoutingPolicy routingPolicy;
  private final RiskEventRecorder riskEventRecorder;
  private final CircuitBreaker deepSeekCircuitBreaker;
  private final boolean failoverEnabled;
  private final String failoverTargetModel;
  private final String defaultChatModel;
  private final ObjectMapper objectMapper;

  public FailoverRoutingAdapter(
      DeepSeekProviderAdapter deepSeekProviderAdapter,
      ZhipuProviderAdapter zhipuProviderAdapter,
      RoutingPolicy routingPolicy,
      RiskEventRecorder riskEventRecorder,
      CircuitBreakerRegistry circuitBreakerRegistry,
      boolean failoverEnabled,
      String failoverTargetModel,
      String defaultChatModel,
      ObjectMapper objectMapper
  ) {
    this.deepSeekProviderAdapter = deepSeekProviderAdapter;
    this.zhipuProviderAdapter = zhipuProviderAdapter;
    this.routingPolicy = routingPolicy;
    this.riskEventRecorder = riskEventRecorder;
    this.deepSeekCircuitBreaker = circuitBreakerRegistry.circuitBreaker("deepseek");
    this.failoverEnabled = failoverEnabled;
    this.failoverTargetModel = failoverTargetModel;
    this.defaultChatModel = defaultChatModel;
    this.objectMapper = objectMapper;
  }

  @Override
  public String providerCode() {
    return deepSeekProviderAdapter.providerCode();
  }

  @Override
  public JsonNode chat(JsonNode openAiRequestBody) {
    ProviderRoute first = routingPolicy.chooseFirstHop();
    ProviderAdapter primary =
        first == ProviderRoute.ZHIPU && zhipuProviderAdapter.isConfigured()
            ? zhipuProviderAdapter
            : deepSeekProviderAdapter;
    ProviderAdapter secondary = pickSecondary(primary);
    try {
      return invokePrimary(primary, openAiRequestBody);
    } catch (Exception ex) {
      if (!failoverEnabled || secondary == null) {
        rethrowAsRuntime(ex);
        throw new AssertionError("unreachable");
      }
      if (!isRecoverable(ex)) {
        rethrowAsRuntime(ex);
        throw new AssertionError("unreachable");
      }
      JsonNode forAlt = prepareBodyForAdapter(secondary, openAiRequestBody);
      riskEventRecorder.recordProviderFailover(
          nameOf(primary),
          nameOf(secondary),
          rootCauseMessage(ex)
      );
      try {
        return invokeSecondary(secondary, forAlt);
      } catch (Exception e2) {
        rethrowAsRuntime(e2);
        throw new AssertionError("unreachable");
      }
    }
  }

  private static void rethrowAsRuntime(Exception ex) {
    if (ex instanceof RuntimeException re) {
      throw re;
    }
    throw new BusinessException(ErrorCode.INTERNAL, ex.getMessage(), ex);
  }

  private JsonNode invokeSecondary(ProviderAdapter secondary, JsonNode body) throws Exception {
    if (secondary == deepSeekProviderAdapter) {
      return deepSeekCircuitBreaker.executeCallable(() -> deepSeekProviderAdapter.chat(body));
    }
    return secondary.chat(body);
  }

  private JsonNode invokePrimary(ProviderAdapter primary, JsonNode body) throws Exception {
    JsonNode prepared = prepareBodyForAdapter(primary, body);
    if (primary == deepSeekProviderAdapter) {
      return deepSeekCircuitBreaker.executeCallable(() -> deepSeekProviderAdapter.chat(prepared));
    }
    return primary.chat(prepared);
  }

  private ProviderAdapter pickSecondary(ProviderAdapter primary) {
    if (primary == deepSeekProviderAdapter && zhipuProviderAdapter.isConfigured()) {
      return zhipuProviderAdapter;
    }
    if (primary == zhipuProviderAdapter) {
      return deepSeekProviderAdapter;
    }
    return null;
  }

  private static String nameOf(ProviderAdapter a) {
    return a.providerCode();
  }

  private JsonNode prepareBodyForAdapter(ProviderAdapter target, JsonNode openAiRequestBody) {
    JsonNode copy = openAiRequestBody.deepCopy();
    if (!(copy instanceof ObjectNode obj)) {
      return copy;
    }
    if (target == zhipuProviderAdapter) {
      obj.put("model", failoverTargetModel);
    } else if (target == deepSeekProviderAdapter) {
      if (!obj.hasNonNull("model") || obj.get("model").asText().isBlank()) {
        obj.put("model", defaultChatModel);
      }
    }
    return obj;
  }

  @Override
  public JsonNode listModels() {
    ArrayNode merged = objectMapper.createArrayNode();
    appendData(merged, deepSeekProviderAdapter.listModels());
    if (zhipuProviderAdapter.isConfigured()) {
      appendData(merged, zhipuProviderAdapter.listModels());
    }
    ObjectNode root = objectMapper.createObjectNode();
    root.put("object", "list");
    root.set("data", merged);
    return root;
  }

  private static void appendData(ArrayNode target, JsonNode chunk) {
    if (chunk == null || !chunk.has("data") || !chunk.get("data").isArray()) {
      return;
    }
    for (JsonNode n : chunk.get("data")) {
      target.add(n);
    }
  }

  private static boolean isRecoverable(Throwable ex) {
    Throwable cur = ex;
    for (int i = 0; i < 10 && cur != null; i++) {
      if (cur instanceof CallNotPermittedException) {
        return true;
      }
      if (cur instanceof BusinessException be) {
        ErrorCode c = be.getErrorCode();
        if (c == ErrorCode.BAD_REQUEST) {
          return false;
        }
        if (c == ErrorCode.INTERNAL || c == ErrorCode.TOO_MANY_REQUESTS) {
          return true;
        }
        return false;
      }
      if (cur instanceof org.springframework.web.client.ResourceAccessException) {
        return true;
      }
      if (cur instanceof org.springframework.web.client.HttpStatusCodeException httpEx) {
        HttpStatusCode s = httpEx.getStatusCode();
        return s.is5xxServerError() || s.value() == 429;
      }
      cur = cur.getCause();
    }
    return false;
  }

  private static String rootCauseMessage(Throwable ex) {
    Throwable c = ex;
    while (c.getCause() != null && c.getCause() != c) {
      c = c.getCause();
    }
    String m = c.getMessage();
    return m != null && !m.isBlank() ? m : c.getClass().getSimpleName();
  }
}
