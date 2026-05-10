package com.tokenhub.gateway.infrastructure.web;

import com.tokenhub.gateway.infrastructure.json.GatewayJsonResponses;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 调用 billing 余额预检（POST /v1/chat/completions 前阻断余额为 0）。
 */
@Component
public class BillingPreflightGatewayFilter implements GlobalFilter, Ordered {

  private final WebClient webClient;

  @Value("${tokenhub.gateway.billing-base-url}")
  private String billingBaseUrl;

  @Value("${tokenhub.gateway.internal-token}")
  private String internalToken;

  public BillingPreflightGatewayFilter(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.build();
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
    var request = exchange.getRequest();
    if (!HttpMethod.POST.matches(request.getMethod().name())) {
      return chain.filter(exchange);
    }
    String path = request.getPath().pathWithinApplication().value();
    if (!"/v1/chat/completions".equals(path)) {
      return chain.filter(exchange);
    }
    String userId = request.getHeaders().getFirst(GatewayIngressHeaders.USER_ID);
    if (userId == null || userId.isBlank()) {
      return chain.filter(exchange);
    }
    final long uid;
    try {
      uid = Long.parseLong(userId.trim());
    } catch (NumberFormatException ex) {
      return chain.filter(exchange);
    }
    String base = trimTrailingSlash(billingBaseUrl);
    String url = base + "/internal/billing/preflight";

    return webClient
        .post()
        .uri(url)
        .header("X-Internal-Token", internalToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("userId", uid))
        .retrieve()
        .toBodilessEntity()
        .then(chain.filter(exchange))
        .onErrorResume(WebClientResponseException.class, ex -> {
          String traceId = Objects.toString(exchange.getAttribute(TraceGatewayFilter.TRACE_ATTR), "");
          if (ex.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) {
            return GatewayJsonResponses.writeBusiness(
                exchange.getResponse(),
                HttpStatus.PAYMENT_REQUIRED.value(),
                traceId,
                "B402001",
                "余额不足，请先充值"
            );
          }
          return Mono.error(ex);
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

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 15;
  }
}
