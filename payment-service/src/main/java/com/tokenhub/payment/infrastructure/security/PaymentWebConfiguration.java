package com.tokenhub.payment.infrastructure.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PaymentWebConfiguration implements WebMvcConfigurer {

  private final PaymentJwtMvcInterceptor interceptor;

  public PaymentWebConfiguration(PaymentJwtMvcInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry
        .addInterceptor(interceptor)
        .addPathPatterns("/payments/**")
        .excludePathPatterns("/payments/mock/callback");
  }
}
