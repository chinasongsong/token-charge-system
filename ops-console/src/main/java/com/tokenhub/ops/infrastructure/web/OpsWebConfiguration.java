package com.tokenhub.ops.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OpsWebConfiguration implements WebMvcConfigurer {

  private final OpsTokenMvcInterceptor opsTokenMvcInterceptor;

  public OpsWebConfiguration(OpsTokenMvcInterceptor opsTokenMvcInterceptor) {
    this.opsTokenMvcInterceptor = opsTokenMvcInterceptor;
  }

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(opsTokenMvcInterceptor).addPathPatterns("/ops/**");
  }
}
