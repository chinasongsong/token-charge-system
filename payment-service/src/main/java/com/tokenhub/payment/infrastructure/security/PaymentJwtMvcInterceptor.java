package com.tokenhub.payment.infrastructure.security;

import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.payment.domain.auth.PaymentAuthConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PaymentJwtMvcInterceptor implements HandlerInterceptor {

  private final PaymentJwtPrincipalResolver resolver;

  public PaymentJwtMvcInterceptor(PaymentJwtPrincipalResolver resolver) {
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
    request.setAttribute(PaymentAuthConstants.REQUEST_USER_ID, uid.get());
    return true;
  }
}
