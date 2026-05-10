package com.tokenhub.gateway.infrastructure.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tokenhub.common.security.apikey.ApiKeySupport;
import com.tokenhub.gateway.infrastructure.json.GatewayJsonResponses;
import java.util.Objects;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 非 JWT 形态的 Bearer 视为平台 API Key（SHA-256 指纹解析到用户），并注入 {@code X-User-Id} / {@code X-Api-Key-Id}。
 */
@Component
public class BillingApiKeyResolveGatewayFilter implements GlobalFilter, Ordered {

  private final WebClient webClient;

  @Value("${tokenhub.gateway.billing-base-url}")
  private String billingBaseUrl;

  @Value("${tokenhub.gateway.internal-token}")
  private String internalToken;

  public BillingApiKeyResolveGatewayFilter(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.build();
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

    String bearer = ApiKeySupport.extractBearer(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    if (bearer == null || bearer.isBlank()) {
      return chain.filter(exchange);
    }
    if (looksLikeJwt(bearer)) {
      return chain.filter(exchange);
    }

    String fingerprint = ApiKeySupport.sha256HexUtf8(bearer);
    String base = trimTrailingSlash(billingBaseUrl);
    String url = base + "/internal/api-keys/by-fingerprint/" + fingerprint;

    return webClient
        .get()
        .uri(url)
        .header("X-Internal-Token", internalToken)
        .retrieve()
        .bodyToMono(ApiKeyResolveBody.class)
        .flatMap(body -> {
          ServerHttpRequest mutated = request.mutate()
              .header(GatewayIngressHeaders.USER_ID, String.valueOf(body.userId()))
              .header(GatewayIngressHeaders.API_KEY_ID, String.valueOf(body.apiKeyId()))
              .build();
          return chain.filter(exchange.mutate().request(mutated).build());
        })
        .onErrorResume(ex -> {
          String traceId = Objects.toString(exchange.getAttribute(TraceGatewayFilter.TRACE_ATTR), "");
          return GatewayJsonResponses.writeBusiness(
              exchange.getResponse(),
              HttpStatus.UNAUTHORIZED.value(),
              traceId,
              "I401001",
              "无效的 API Key"
          );
        });
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

  private static boolean looksLikeJwt(String bearer) {
    int dots = 0;
    for (int i = 0; i < bearer.length(); i++) {
      if (bearer.charAt(i) == '.') {
        dots++;
      }
    }
    return dots == 2;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ApiKeyResolveBody(long userId, long apiKeyId) {}

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 11;
  }
}
