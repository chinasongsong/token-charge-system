package com.tokenhub.common.core.trace;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Ensures every inbound request has a trace id (header or generated) and binds it to MDC.
 */
public class TraceIdInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler
  ) {
    String incoming = request.getHeader(TraceIdConstants.HEADER_NAME);
    String traceId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
    TraceContext.setTraceId(traceId);
    response.setHeader(TraceIdConstants.HEADER_NAME, traceId);
    return true;
  }

  @Override
  public void afterCompletion(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      Exception ex
  ) {
    TraceContext.clear();
  }
}
