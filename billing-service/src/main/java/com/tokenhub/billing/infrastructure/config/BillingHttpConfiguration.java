package com.tokenhub.billing.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BillingHttpConfiguration {

  @Bean
  @Qualifier("billingRestTemplate")
  RestTemplate billingRestTemplate() {
    return new RestTemplate();
  }
}
