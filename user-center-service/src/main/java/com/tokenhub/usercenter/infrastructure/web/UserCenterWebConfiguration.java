package com.tokenhub.usercenter.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UserCenterWebConfiguration implements WebMvcConfigurer {

  private final UserJwtMvcInterceptor userJwtMvcInterceptor;

  public UserCenterWebConfiguration(UserJwtMvcInterceptor userJwtMvcInterceptor) {
    this.userJwtMvcInterceptor = userJwtMvcInterceptor;
  }

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(userJwtMvcInterceptor).addPathPatterns("/user/me");
  }
}
