package com.tokenhub.ops.infrastructure.web;

import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OpsTokenMvcInterceptor implements HandlerInterceptor {

  @Value("${tokenhub.ops.internal-token:dev-ops-token}")
  private String internalToken;

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler
  ) {
    String provided = request.getHeader("X-Ops-Token");
    if (provided == null || !provided.equals(internalToken)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "X-Ops-Token 无效");
    }
    return true;
  }
}
