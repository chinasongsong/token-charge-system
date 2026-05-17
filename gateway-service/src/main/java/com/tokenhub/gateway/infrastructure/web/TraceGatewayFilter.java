package com.tokenhub.gateway.infrastructure.web;

import com.tokenhub.gateway.infrastructure.json.GatewayJsonResponses;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
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

  public static final int TRACE_ID_MAX_LENGTH = 128;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String incoming = request.getHeaders().getFirst(TRACE_HEADER);
    String traceId;
    if (incoming == null || incoming.isBlank()) {
      traceId = UUID.randomUUID().toString();
    } else {
      traceId = incoming.trim();
      if (traceId.length() > TRACE_ID_MAX_LENGTH) {
        return GatewayJsonResponses.writeBusiness(
            exchange.getResponse(),
            HttpStatus.BAD_REQUEST.value(),
            traceId.substring(0, TRACE_ID_MAX_LENGTH),
            "I400002",
            "X-Trace-Id 长度不能超过 " + TRACE_ID_MAX_LENGTH
        );
      }
    }

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
