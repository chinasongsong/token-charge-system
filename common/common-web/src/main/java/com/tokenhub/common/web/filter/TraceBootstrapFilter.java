package com.tokenhub.common.web.filter;

import com.tokenhub.common.core.trace.TraceContext;
import com.tokenhub.common.core.trace.TraceIdConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Binds trace id for the servlet thread for the entire filter chain lifecycle (logging + MVC).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceBootstrapFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    String incoming = request.getHeader(TraceIdConstants.HEADER_NAME);
    String traceId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
    TraceContext.setTraceId(traceId);
    response.setHeader(TraceIdConstants.HEADER_NAME, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      TraceContext.clear();
    }
  }
}
