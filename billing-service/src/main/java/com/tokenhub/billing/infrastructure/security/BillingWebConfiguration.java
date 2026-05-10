package com.tokenhub.billing.infrastructure.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BillingWebConfiguration implements WebMvcConfigurer {

  private final BillingJwtMvcInterceptor jwtInterceptor;

  public BillingWebConfiguration(BillingJwtMvcInterceptor jwtInterceptor) {
    this.jwtInterceptor = jwtInterceptor;
  }

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(jwtInterceptor)
        .addPathPatterns("/apikeys", "/apikeys/**", "/dashboard/**", "/billing/**", "/v1/usage")
        .excludePathPatterns("/internal/**");
  }
}
