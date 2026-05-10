package com.tokenhub.adapter.infrastructure.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenhub.adapter.domain.provider.ProviderAdapter;
import com.tokenhub.adapter.infrastructure.deepseek.DeepSeekProviderAdapter;
import com.tokenhub.adapter.domain.routing.RoutingPolicy;
import com.tokenhub.adapter.infrastructure.risk.RiskEventRecorder;
import com.tokenhub.adapter.infrastructure.zhipu.ZhipuProviderAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AdapterRoutingConfiguration {

  @Bean
  @Primary
  public ProviderAdapter failoverRoutingAdapter(
      DeepSeekProviderAdapter deepSeekProviderAdapter,
      ZhipuProviderAdapter zhipuProviderAdapter,
      RoutingPolicy routingPolicy,
      RiskEventRecorder riskEventRecorder,
      CircuitBreakerRegistry circuitBreakerRegistry,
      ObjectMapper objectMapper,
      @Value("${tokenhub.adapter.failover-enabled:true}") boolean failoverEnabled,
      @Value("${tokenhub.adapter.zhipu-default-chat-model:glm-4-flash}") String zhipuDefaultChatModel,
      @Value("${tokenhub.adapter.default-chat-model:deepseek-v4-flash}") String defaultDashModel
  ) {
    return new FailoverRoutingAdapter(
        deepSeekProviderAdapter,
        zhipuProviderAdapter,
        routingPolicy,
        riskEventRecorder,
        circuitBreakerRegistry,
        failoverEnabled,
        zhipuDefaultChatModel,
        defaultDashModel,
        objectMapper
    );
  }
}
