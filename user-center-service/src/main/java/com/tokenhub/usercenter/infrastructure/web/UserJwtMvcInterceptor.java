package com.tokenhub.usercenter.infrastructure.web;

import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.usercenter.infrastructure.security.JwtPrincipalResolver;
import com.tokenhub.usercenter.domain.auth.AuthConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserJwtMvcInterceptor implements HandlerInterceptor {

  private final JwtPrincipalResolver jwtPrincipalResolver;

  public UserJwtMvcInterceptor(JwtPrincipalResolver jwtPrincipalResolver) {
    this.jwtPrincipalResolver = jwtPrincipalResolver;
  }

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler
  ) {
    Optional<Long> uid = jwtPrincipalResolver.resolveBearer(request.getHeader("Authorization"));
    if (uid.isEmpty()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或令牌无效");
    }
    request.setAttribute(AuthConstants.REQUEST_USER_ID, uid.get());
    return true;
  }
}
