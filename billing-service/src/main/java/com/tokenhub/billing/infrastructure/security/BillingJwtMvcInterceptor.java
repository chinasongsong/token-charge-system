package com.tokenhub.billing.infrastructure.security;

import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.billing.domain.auth.BillingAuthConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class BillingJwtMvcInterceptor implements HandlerInterceptor {

  private final BillingJwtPrincipalResolver resolver;

  public BillingJwtMvcInterceptor(BillingJwtPrincipalResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler
  ) {
    Optional<Long> uid = resolver.resolveBearer(request.getHeader("Authorization"));
    if (uid.isEmpty()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或令牌无效");
    }
    request.setAttribute(BillingAuthConstants.REQUEST_USER_ID, uid.get());
    return true;
  }
}
