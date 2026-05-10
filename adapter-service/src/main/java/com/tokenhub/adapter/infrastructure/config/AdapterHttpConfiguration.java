package com.tokenhub.adapter.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AdapterHttpConfiguration {

  @Bean
  RestTemplate adapterRestTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(15))
        .setReadTimeout(Duration.ofMinutes(3))
        .build();
  }
}
