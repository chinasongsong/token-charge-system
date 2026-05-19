package com.tokenhub.gateway.infrastructure.web;

public final class GatewayIngressHeaders {

  public static final String USER_ID = "X-User-Id";
  public static final String API_KEY_ID = "X-Api-Key-Id";
  public static final String TRACE_ID = "X-Trace-Id";
  public static final String IDEMPOTENCY_COMPOSITE = "X-Idempotency-Key-Composite";
  public static final String IDEMPOTENCY_SOURCE = "X-Idempotency-Source";

  /** JWT 路径无 API Key 时复合幂等键中的 apiKeyId 占位。 */
  public static final String API_KEY_ID_JWT_PLACEHOLDER = "0";

  private GatewayIngressHeaders() {}
}
