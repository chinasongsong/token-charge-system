package com.tokenhub.gateway.infrastructure.web;

import com.tokenhub.gateway.infrastructure.json.GatewayJsonResponses;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * O-6 网关风控：IP 黑白名单 + 日级别配额（按 apiKeyId 维度）。
 *
 * <ul>
 *   <li>IP 黑名单：Redis set {@code risk:ip:deny}（运营配置）。命中即 403。
 *   <li>IP 白名单：Redis set {@code risk:ip:allow}。配置 {@code enforce-allowlist=true} 时仅允许列内 IP；
 *       否则默认放行。
 *   <li>日配额：Redis 计数 {@code rl:daily:{apiKeyId}:{yyyyMMdd}}，超过默认值返回 429。
 * </ul>
 *
 * <p>顺序：在 {@link BillingApiKeyResolveGatewayFilter}（解析 API Key 后）之后、{@link ApiKeyRedisRateLimitGatewayFilter} 之前。
 */
@Component
public class IpRiskAndQuotaGatewayFilter implements GlobalFilter, Ordered {

  private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final ReactiveStringRedisTemplate redis;

  @Value("${tokenhub.gateway.risk.enabled:false}")
  private boolean enabled;

  @Value("${tokenhub.gateway.risk.enforce-allowlist:false}")
  private boolean enforceAllowlist;

  @Value("${tokenhub.gateway.risk.daily-quota:0}")
  private long dailyQuota;

  public IpRiskAndQuotaGatewayFilter(ReactiveStringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
    if (!enabled) {
      return chain.filter(exchange);
    }
    ServerHttpRequest request = exchange.getRequest();
    if (HttpMethod.OPTIONS.matches(request.getMethod().name())) {
      return chain.filter(exchange);
    }
    String path = request.getPath().pathWithinApplication().value();
    if (!path.startsWith("/v1/")) {
      return chain.filter(exchange);
    }

    String ip = resolveClientIp(request);
    return checkIp(ip)
        .flatMap(decision -> {
          if (decision == IpDecision.DENY) {
            return reject(exchange, HttpStatus.FORBIDDEN, "I403002", "IP 已被拒绝");
          }
          if (decision == IpDecision.NOT_ALLOWED) {
            return reject(exchange, HttpStatus.FORBIDDEN, "I403003", "IP 不在白名单");
          }
          return checkDailyQuota(exchange, chain);
        });
  }

  private enum IpDecision {
    DENY,
    NOT_ALLOWED,
    OK
  }

  private Mono<IpDecision> checkIp(String ip) {
    if (ip == null || ip.isBlank()) {
      return Mono.just(IpDecision.OK);
    }
    Mono<Boolean> denied = redis.opsForSet().isMember("risk:ip:deny", ip).defaultIfEmpty(Boolean.FALSE);
    return denied.flatMap(d -> {
      if (Boolean.TRUE.equals(d)) {
        return Mono.just(IpDecision.DENY);
      }
      if (!enforceAllowlist) {
        return Mono.just(IpDecision.OK);
      }
      return redis.opsForSet().isMember("risk:ip:allow", ip)
          .defaultIfEmpty(Boolean.FALSE)
          .map(allowed -> Boolean.TRUE.equals(allowed) ? IpDecision.OK : IpDecision.NOT_ALLOWED);
    }).onErrorReturn(IpDecision.OK);
  }

  private Mono<Void> checkDailyQuota(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (dailyQuota <= 0) {
      return chain.filter(exchange);
    }
    String apiKeyId = exchange.getRequest().getHeaders().getFirst(GatewayIngressHeaders.API_KEY_ID);
    if (apiKeyId == null || apiKeyId.isBlank()) {
      return chain.filter(exchange);
    }
    String day = LocalDate.now().format(DAY_FORMAT);
    String key = "rl:daily:" + apiKeyId.trim() + ":" + day;
    return redis.opsForValue().increment(key)
        .flatMap(count -> {
          if (count != null && count == 1L) {
            return redis.expire(key, Duration.ofDays(2)).thenReturn(count);
          }
          return Mono.just(count == null ? 0L : count);
        })
        .flatMap(count -> {
          if (count != null && count > dailyQuota) {
            return reject(exchange, HttpStatus.TOO_MANY_REQUESTS, "I429002", "日配额已耗尽");
          }
          return chain.filter(exchange);
        })
        .onErrorResume(ex -> chain.filter(exchange));
  }

  private static Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String code, String message) {
    String traceId = Objects.toString(exchange.getAttribute(TraceGatewayFilter.TRACE_ATTR), "");
    return GatewayJsonResponses.writeBusiness(
        exchange.getResponse(),
        status.value(),
        traceId,
        code,
        message
    );
  }

  private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

  private static String resolveClientIp(ServerHttpRequest request) {
    String forwarded = request.getHeaders().getFirst(HEADER_X_FORWARDED_FOR);
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
    }
    InetSocketAddress addr = request.getRemoteAddress();
    return addr != null && addr.getAddress() != null ? addr.getAddress().getHostAddress() : null;
  }

  @Override
  public int getOrder() {
    // 解析 API Key 后（+11）、限流（+13）之前
    return Ordered.HIGHEST_PRECEDENCE + 12;
  }
}
