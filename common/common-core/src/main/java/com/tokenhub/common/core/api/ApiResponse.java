package com.tokenhub.common.core.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.common.core.trace.TraceContext;
import java.time.Instant;

/**
 * Unified HTTP envelope for MVC services.
 *
 * @param <T> business payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    String traceId,
    String code,
    String message,
    T data,
    Instant timestamp
) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(
        TraceContext.currentTraceIdOrNull(),
        ErrorCode.SUCCESS.getCode(),
        ErrorCode.SUCCESS.getDefaultMessage(),
        data,
        Instant.now()
    );
  }

  public static ApiResponse<Void> ok() {
    return ok(null);
  }

  public static <T> ApiResponse<T> fail(String code, String message) {
    return new ApiResponse<>(
        TraceContext.currentTraceIdOrNull(),
        code,
        message,
        null,
        Instant.now()
    );
  }
}
