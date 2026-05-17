package com.tokenhub.gateway.infrastructure.web;

import com.tokenhub.gateway.infrastructure.json.GatewayJsonResponses;
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
 * O-10：Chat 幂等键。客户端 {@code X-Idempotency-Key}（UUID v4）或回退 traceId，合成复合键供 billing settle。
 */
@Component
public class IdempotencyGatewayFilter implements GlobalFilter, Ordered {

  public static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
  public static final String IDEMPOTENCY_COMPOSITE_HEADER = GatewayIngressHeaders.IDEMPOTENCY_COMPOSITE;
  public static final String IDEMPOTENCY_SOURCE_HEADER = GatewayIngressHeaders.IDEMPOTENCY_SOURCE;
  public static final String IDEMPOTENCY_ATTR = "gateway.idempotencyKey";
  public static final String IDEMPOTENCY_SOURCE_ATTR = "gateway.idempotencySource";

  public static final String SOURCE_CLIENT = "CLIENT";
  public static final String SOURCE_TRACE_FALLBACK = "TRACE_ID_FALLBACK";

  private static final Pattern UUID_V4_PATTERN = Pattern.compile(
      "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
  );

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();

    if (!HttpMethod.POST.matches(request.getMethod().name())) {
      return chain.filter(exchange);
    }
    String path = request.getPath().pathWithinApplication().value();
    if (!"/v1/chat/completions".equals(path)) {
      return chain.filter(exchange);
    }

    String clientKey = request.getHeaders().getFirst(IDEMPOTENCY_HEADER);
    String userId = request.getHeaders().getFirst(GatewayIngressHeaders.USER_ID);
    String apiKeyId = request.getHeaders().getFirst(GatewayIngressHeaders.API_KEY_ID);
    String traceId = Objects.toString(exchange.getAttribute(TraceGatewayFilter.TRACE_ATTR), "");

    if (userId == null || userId.isBlank()) {
      return chain.filter(exchange);
    }

    String effectiveApiKeyId = (apiKeyId == null || apiKeyId.isBlank())
        ? GatewayIngressHeaders.API_KEY_ID_JWT_PLACEHOLDER
        : apiKeyId.trim();

    String compositeKey;
    String source;

    if (clientKey != null && !clientKey.isBlank()) {
      String trimmed = clientKey.trim();
      if (!isValidUuidV4(trimmed)) {
        return GatewayJsonResponses.writeBusiness(
            exchange.getResponse(),
            HttpStatus.BAD_REQUEST.value(),
            traceId,
            "I400001",
            "幂等键格式非法，必须是 UUID v4 格式：" + maskIdempotencyKey(trimmed)
        );
      }
      compositeKey = buildCompositeKey(userId.trim(), effectiveApiKeyId, trimmed);
      source = SOURCE_CLIENT;
    } else {
      if (traceId.isBlank()) {
        return chain.filter(exchange);
      }
      compositeKey = buildCompositeKey(userId.trim(), effectiveApiKeyId, traceId);
      source = SOURCE_TRACE_FALLBACK;
    }

    ServerHttpRequest mutated = request.mutate()
        .header(IDEMPOTENCY_COMPOSITE_HEADER, compositeKey)
        .header(IDEMPOTENCY_SOURCE_HEADER, source)
        .build();

    exchange.getAttributes().put(IDEMPOTENCY_ATTR, compositeKey);
    exchange.getAttributes().put(IDEMPOTENCY_SOURCE_ATTR, source);

    return chain.filter(exchange.mutate().request(mutated).build());
  }

  private static boolean isValidUuidV4(String key) {
    if (key == null || key.length() != 36) {
      return false;
    }
    return UUID_V4_PATTERN.matcher(key.toLowerCase()).matches();
  }

  private static String buildCompositeKey(String userId, String apiKeyId, String clientKey) {
    return userId + ":" + apiKeyId + ":" + clientKey;
  }

  private static String maskIdempotencyKey(String key) {
    if (key == null || key.length() < 8) {
      return "...(invalid)";
    }
    return "..." + key.substring(key.length() - 8);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 12;
  }
}
