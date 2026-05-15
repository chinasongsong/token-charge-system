package com.tokenhub.billing.infrastructure.schedule;

import com.tokenhub.billing.infrastructure.persistence.ApiKeyLifecycleMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * O-7：定时把到期 ACTIVE API Key 翻转为 EXPIRED。
 *
 * <p>注意：网关侧 {@code BillingApiKeyResolveGatewayFilter} 与 billing 解析路径都已即时校验
 * {@code expires_at}，本任务仅做「最终状态收敛」与运营审计可见性。
 */
@Component
public class ApiKeyExpirationScheduler {

  private static final Logger log = LoggerFactory.getLogger(ApiKeyExpirationScheduler.class);

  private final ApiKeyLifecycleMapper apiKeyLifecycleMapper;

  @Value("${tokenhub.billing.apikey-expiration.enabled:false}")
  private boolean enabled;

  public ApiKeyExpirationScheduler(ApiKeyLifecycleMapper apiKeyLifecycleMapper) {
    this.apiKeyLifecycleMapper = apiKeyLifecycleMapper;
  }

  @Scheduled(cron = "${tokenhub.billing.apikey-expiration.cron:0 */5 * * * ?}")
  public void sweep() {
    if (!enabled) {
      return;
    }
    int flipped = apiKeyLifecycleMapper.markExpired(LocalDateTime.now());
    if (flipped > 0) {
      log.info("api-key expiration sweep: flipped {} ACTIVE->EXPIRED", flipped);
    }
  }
}
