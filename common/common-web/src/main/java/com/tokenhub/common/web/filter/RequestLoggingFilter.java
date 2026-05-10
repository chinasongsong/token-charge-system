package com.tokenhub.common.web.filter;

import com.tokenhub.common.core.trace.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Minimal request log line; trace id is expected to be set by {@link com.tokenhub.common.core.trace.TraceIdInterceptor}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    long started = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } finally {
      long cost = System.currentTimeMillis() - started;
      log.info("{} {} traceId={} status={} costMs={}",
          request.getMethod(),
          request.getRequestURI(),
          TraceContext.currentTraceIdOrNull(),
          response.getStatus(),
          cost
      );
    }
  }
}
