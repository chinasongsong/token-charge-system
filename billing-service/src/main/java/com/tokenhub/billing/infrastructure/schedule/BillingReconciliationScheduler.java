package com.tokenhub.billing.infrastructure.schedule;

import com.tokenhub.billing.infrastructure.persistence.RequestOrderMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日终/定时自检：订单与流水一致性（平台内对账）；与微信/支付宝账单比对在生产接通道后扩展。
 */
@Component
public class BillingReconciliationScheduler {

  private static final Logger log = LoggerFactory.getLogger(BillingReconciliationScheduler.class);

  private final RequestOrderMapper requestOrderMapper;

  @Value("${tokenhub.billing.reconciliation-enabled:false}")
  private boolean enabled;

  @Value("${tokenhub.billing.reconciliation-stale-pending-hours:1}")
  private int stalePendingHours;

  public BillingReconciliationScheduler(RequestOrderMapper requestOrderMapper) {
    this.requestOrderMapper = requestOrderMapper;
  }

  @Scheduled(cron = "${tokenhub.billing.reconciliation-cron:0 0 3 * * ?}")
  public void reconcileDaily() {
    if (!enabled) {
      return;
    }
    LocalDateTime staleBefore = LocalDateTime.now().minusHours(Math.max(1, stalePendingHours));
    long completedMissingLedger = requestOrderMapper.countCompletedMissingUsageLedger();
    long stalePending = requestOrderMapper.countStalePending(staleBefore);
    if (completedMissingLedger > 0 || stalePending > 0) {
      log.warn(
          "billing reconciliation anomalies: completedMissingUsageLedger={}, stalePendingOlderThan{}h={}",
          completedMissingLedger,
          stalePendingHours,
          stalePending
      );
    } else {
      log.info(
          "billing reconciliation ok: completedMissingUsageLedger=0, stalePendingOlderThan{}h=0",
          stalePendingHours
      );
    }
  }
}
