package com.tokenhub.gateway.infrastructure.web;

import com.tokenhub.common.security.apikey.ApiKeySupport;
import com.tokenhub.common.security.jwt.JwtSupport;
import com.tokenhub.gateway.infrastructure.json.GatewayJsonResponses;
import io.jsonwebtoken.Claims;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@code /v1/**} requires non-empty {@code Authorization: Bearer}; JWT-shaped tokens are verified when {@code JWT_SECRET} is set.
 */
@Component
public class V1IngressAuthGatewayFilter implements GlobalFilter, Ordered {

  private static final String USER_ID_HEADER = "X-User-Id";

  private final SecretKey jwtSigningKey;

  public V1IngressAuthGatewayFilter(@Value("${JWT_SECRET:}") String jwtSecretUtf8) {
    if (jwtSecretUtf8 == null || jwtSecretUtf8.isBlank()) {
      this.jwtSigningKey = null;
    } else {
      this.jwtSigningKey = JwtSupport.hmacShaKeyFromUtf8(jwtSecretUtf8, 32);
    }
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    if (HttpMethod.OPTIONS.matches(request.getMethod().name())) {
      return chain.filter(exchange);
    }
    if (!HttpMethod.GET.matches(request.getMethod().name())
        && !HttpMethod.POST.matches(request.getMethod().name())
        && !HttpMethod.PATCH.matches(request.getMethod().name())) {
      return chain.filter(exchange);
    }
    String path = request.getPath().pathWithinApplication().value();
    if (!path.startsWith("/v1/")) {
      return chain.filter(exchange);
    }

    String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    String bearer = ApiKeySupport.extractBearer(auth);
    if (bearer == null || bearer.isBlank()) {
      return unauthorized(exchange, "缺少 Authorization Bearer");
    }

    if (looksLikeJwt(bearer)) {
      if (jwtSigningKey == null) {
        return chain.filter(exchange);
      }
      try {
        Claims claims = JwtSupport.parse(bearer, jwtSigningKey);
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
          return unauthorized(exchange, "令牌无效");
        }
        ServerHttpRequest mutated = exchange.getRequest().mutate().header(USER_ID_HEADER, sub).build();
        return chain.filter(exchange.mutate().request(mutated).build());
      } catch (RuntimeException ex) {
        return unauthorized(exchange, "未登录或令牌无效");
      }
    }

    return chain.filter(exchange);
  }

  private static boolean looksLikeJwt(String bearer) {
    int dots = 0;
    for (int i = 0; i < bearer.length(); i++) {
      if (bearer.charAt(i) == '.') {
        dots++;
      }
    }
    return dots == 2;
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
    String traceId = Objects.toString(exchange.getAttribute(TraceGatewayFilter.TRACE_ATTR), "");
    return GatewayJsonResponses.writeBusiness(
        exchange.getResponse(),
        HttpStatus.UNAUTHORIZED.value(),
        traceId,
        "I401001",
        message
    );
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
