package com.tokenhub.billing.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalApiGuardFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Internal-Token";

  @Value("${BILLING_INTERNAL_TOKEN:dev-internal-token}")
  private String expectedToken;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    String uri = request.getRequestURI();
    if (uri == null || !uri.startsWith("/internal/")) {
      filterChain.doFilter(request, response);
      return;
    }
    String presented = request.getHeader(HEADER);
    if (presented == null || !presented.equals(expectedToken)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "invalid internal token");
      return;
    }
    filterChain.doFilter(request, response);
  }
}
