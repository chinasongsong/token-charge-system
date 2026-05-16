package com.tokenhub.gateway.infrastructure.web;

import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 客户端幂等键处理：
 * 1. 校验 X-Idempotency-Key 格式（必须是 UUID v4）
 * 2. 构建复合幂等键：userId:apiKeyId:clientKey
 * 3. 若客户端未提供，回退使用 traceId
 * 4. 注入 X-Idempotency-Key-Composite 和 X-Idempotency-Source Header
 */
@Component
public class IdempotencyGatewayFilter implements GlobalFilter, Ordered {

  public static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
  public static final String IDEMPOTENCY_COMPOSITE_HEADER = "X-Idempotency-Key-Composite";
  public static final String IDEMPOTENCY_SOURCE_HEADER = "X-Idempotency-Source";
  public static final String IDEMPOTENCY_ATTR = "gateway.idempotencyKey";
  public static final String IDEMPOTENCY_SOURCE_ATTR = "gateway.idempotencySource";

  // UUID v4 格式校验：xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx，其中 y ∈ [8,9,a,b]
  private static final Pattern UUID_V4_PATTERN = Pattern.compile(
      "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
  );

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();

    // 仅处理 POST /v1/chat/completions
    if (!HttpMethod.POST.matches(request.getMethod().name())) {
      return chain.filter(exchange);
    }
    String path = request.getPath().pathWithinApplication().value();
    if (!"/v1/chat/completions".equals(path)) {
      return chain.filter(exchange);
    }

    // 获取客户端提供的幂等键
    String clientKey = request.getHeaders().getFirst(IDEMPOTENCY_HEADER);

    // 获取 userId 和 apiKeyId（由 BillingApiKeyResolveGatewayFilter 注入）
    String userId = request.getHeaders().getFirst(GatewayIngressHeaders.USER_ID);
    String apiKeyId = request.getHeaders().getFirst(GatewayIngressHeaders.API_KEY_ID);

    // 获取 traceId（由 TraceGatewayFilter 注入）
    String traceId = Objects.toString(exchange.getAttribute(TraceGatewayFilter.TRACE_ATTR), "");

    // 若 userId/apiKeyId 缺失（非 API Key 请求），直接放行
    if (userId == null || userId.isBlank() || apiKeyId == null || apiKeyId.isBlank()) {
      return chain.filter(exchange);
    }

    // 构建复合幂等键
    String compositeKey;
    String source;

    if (clientKey != null && !clientKey.isBlank()) {
      // 客户端提供了幂等键，校验格式
      if (!isValidUuidV4(clientKey)) {
        String maskedKey = maskIdempotencyKey(clientKey);
        return GatewayJsonResponses.writeBusiness(
            exchange.getResponse(),
            HttpStatus.BAD_REQUEST.value(),
            traceId,
            "I400001",
            "幂等键格式非法，必须是 UUID v4 格式：" + maskedKey
        );
      }
      compositeKey = buildCompositeKey(userId, apiKeyId, clientKey.trim());
      source = "CLIENT";
    } else {
      // 客户端未提供幂等键，回退使用 traceId
      compositeKey = buildCompositeKey(userId, apiKeyId, traceId);
      source = "TRACE_ID_FALLBACK";
    }

    // 注入复合幂等键和来源
    ServerHttpRequest mutated = request.mutate()
        .header(IDEMPOTENCY_COMPOSITE_HEADER, compositeKey)
        .header(IDEMPOTENCY_SOURCE_HEADER, source)
        .build();

    exchange.getAttributes().put(IDEMPOTENCY_ATTR, compositeKey);
    exchange.getAttributes().put(IDEMPOTENCY_SOURCE_ATTR, source);

    return chain.filter(exchange.mutate().request(mutated).build());
  }

  /**
   * 校验 UUID v4 格式
   */
  private boolean isValidUuidV4(String key) {
    if (key == null || key.length() != 36) {
      return false;
    }
    return UUID_V4_PATTERN.matcher(key.toLowerCase()).matches();
  }

  /**
   * 构建复合幂等键：userId:apiKeyId:clientKey
   */
  private String buildCompositeKey(String userId, String apiKeyId, String clientKey) {
    return userId + ":" + apiKeyId + ":" + clientKey;
  }

  /**
   * 脱敏幂等键（仅显示后 8 位）
   */
  private String maskIdempotencyKey(String key) {
    if (key == null || key.length() < 8) {
      return "...(invalid)";
    }
    return "..." + key.substring(key.length() - 8);
  }

  @Override
  public int getOrder() {
    // 在 BillingApiKeyResolveGatewayFilter (order=11) 之后执行
    return Ordered.HIGHEST_PRECEDENCE + 12;
  }
}