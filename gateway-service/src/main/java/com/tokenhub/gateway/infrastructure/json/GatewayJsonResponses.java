package com.tokenhub.gateway.infrastructure.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

public final class GatewayJsonResponses {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private GatewayJsonResponses() {}

  public static Mono<Void> writeBusiness(ServerHttpResponse response, int httpStatus, String traceId, String code, String message) {
    response.setRawStatusCode(httpStatus);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("traceId", traceId);
    body.put("code", code);
    body.put("message", message);
    body.put("data", null);
    body.put("timestamp", Instant.now().toString());
    byte[] bytes;
    try {
      bytes = MAPPER.writeValueAsBytes(body);
    } catch (JsonProcessingException e) {
      return Mono.error(e);
    }
    return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
  }
}
