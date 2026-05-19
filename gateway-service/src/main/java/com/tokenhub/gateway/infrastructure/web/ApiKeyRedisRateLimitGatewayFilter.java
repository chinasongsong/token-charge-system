package com.tokenhub.gateway.infrastructure.web;

import com.tokenhub.gateway.infrastructure.json.GatewayJsonResponses;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 按秒桶限流：优先使用 {@code X-Api-Key-Id}，否则用 {@code X-User-Id}。
 */
@Component
public class ApiKeyRedisRateLimitGatewayFilter implements GlobalFilter, Ordered {

  private final ReactiveStringRedisTemplate redis;

  @Value("${tokenhub.gateway.rate-limit-per-second:60}")
  private int perSecond;

  public ApiKeyRedisRateLimitGatewayFilter(ReactiveStringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
    var request = exchange.getRequest();
    if (!HttpMethod.POST.matches(request.getMethod().name())) {
      return chain.filter(exchange);
    }
    String path = request.getPath().pathWithinApplication().value();
    if (!path.startsWith("/v1/")) {
      return chain.filter(exchange);
    }

    String apiKeyId = request.getHeaders().getFirst(GatewayIngressHeaders.API_KEY_ID);
    String userId = request.getHeaders().getFirst(GatewayIngressHeaders.USER_ID);
    String suffix;
    if (apiKeyId != null && !apiKeyId.isBlank()) {
      suffix = "ak:" + apiKeyId.trim();
    } else if (userId != null && !userId.isBlank()) {
      suffix = "uid:" + userId.trim();
    } else {
      return chain.filter(exchange);
    }

    long sec = Instant.now().getEpochSecond();
    String key = "rl:" + suffix + ":s:" + sec;

    return redis.opsForValue()
        .increment(key)
        .flatMap(count -> {
          if (count != null && count == 1L) {
            return redis.expire(key, Duration.ofSeconds(3)).thenReturn(count);
          }
          return Mono.just(count);
        })
        .flatMap(count -> {
          if (count != null && count > perSecond) {
            String traceId = Objects.toString(exchange.getAttribute(TraceGatewayFilter.TRACE_ATTR), "");
            return GatewayJsonResponses.writeBusiness(
                exchange.getResponse(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                traceId,
                "I429001",
                "请求过于频繁"
            );
          }
          return chain.filter(exchange);
        });
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 14;
  }
}
