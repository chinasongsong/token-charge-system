package com.tokenhub.billing.infrastructure.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日终对账占位：与三方账单比对在生产接通道后扩展。
 */
@Component
public class BillingReconciliationScheduler {

  private static final Logger log = LoggerFactory.getLogger(BillingReconciliationScheduler.class);

  @Value("${tokenhub.billing.reconciliation-enabled:false}")
  private boolean enabled;

  @Scheduled(cron = "${tokenhub.billing.reconciliation-cron:0 0 3 * * ?}")
  public void reconcileDaily() {
    if (!enabled) {
      return;
    }
    log.info("billing reconciliation job (placeholder)");
  }
}
