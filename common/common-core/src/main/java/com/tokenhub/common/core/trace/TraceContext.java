package com.tokenhub.common.core.trace;

import org.slf4j.MDC;

/**
 * Propagates trace identifiers through logging MDC.
 */
public final class TraceContext {

  private TraceContext() {}

  public static void setTraceId(String traceId) {
    if (traceId == null || traceId.isBlank()) {
      return;
    }
    MDC.put(TraceIdConstants.MDC_KEY, traceId);
  }

  public static void clear() {
    MDC.remove(TraceIdConstants.MDC_KEY);
  }

  public static String currentTraceIdOrNull() {
    return MDC.get(TraceIdConstants.MDC_KEY);
  }
}
