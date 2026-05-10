package com.tokenhub.gateway.infrastructure.web;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceGatewayFilter implements GlobalFilter, Ordered {

  public static final String TRACE_HEADER = "X-Trace-Id";
  public static final String TRACE_ATTR = "gateway.traceId";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String incoming = request.getHeaders().getFirst(TRACE_HEADER);
    String traceId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming.trim();

    ServerHttpRequest mutated = request.mutate().header(TRACE_HEADER, traceId).build();
    exchange.getAttributes().put(TRACE_ATTR, traceId);

    ServerHttpResponse response = exchange.getResponse();
    response.beforeCommit(() -> {
      response.getHeaders().set(TRACE_HEADER, traceId);
      return Mono.empty();
    });

    return chain.filter(exchange.mutate().request(mutated).build());
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
