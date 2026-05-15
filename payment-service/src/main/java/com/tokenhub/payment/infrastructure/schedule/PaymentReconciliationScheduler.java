package com.tokenhub.payment.infrastructure.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.payment.infrastructure.persistence.PaymentOrderMapper;
import com.tokenhub.payment.infrastructure.persistence.PaymentOrderPo;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 支付侧对账与观测：统计 INIT 订单；不自动入账（未支付 INIT 与回调丢失无法区分，自动入账会造成盗刷）。
 * 在确认渠道已收款后，使用 {@code POST /internal/payments/orders/retry-credit} 触发幂等入账。
 */
@Component
public class PaymentReconciliationScheduler {

  private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationScheduler.class);

  private final PaymentOrderMapper paymentOrderMapper;

  @Value("${tokenhub.payment.reconcile-enabled:false}")
  private boolean enabled;

  @Value("${tokenhub.payment.reconcile-stale-init-warn-minutes:60}")
  private int staleInitWarnMinutes;

  public PaymentReconciliationScheduler(PaymentOrderMapper paymentOrderMapper) {
    this.paymentOrderMapper = paymentOrderMapper;
  }

  @Scheduled(cron = "${tokenhub.payment.reconcile-cron:0 */10 * * * ?}")
  public void reportPendingInitOrders() {
    if (!enabled) {
      return;
    }
    List<PaymentOrderPo> pending =
        paymentOrderMapper.selectList(
            new LambdaQueryWrapper<PaymentOrderPo>()
                .eq(PaymentOrderPo::getStatus, "INIT")
                .last("LIMIT 500")
        );
    int total = pending.size();
    LocalDateTime threshold = LocalDateTime.now().minusMinutes(Math.max(1, staleInitWarnMinutes));
    long stale =
        pending.stream().filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isBefore(threshold)).count();
    if (stale > 0) {
      log.warn(
          "payment reconcile: INIT orders total={}, olderThan{}Minutes={} (review channel state; retry credit via internal API if paid)",
          total,
          staleInitWarnMinutes,
          stale
      );
    } else {
      log.info("payment reconcile: INIT orders total={}, no stale backlog by age threshold", total);
    }
  }
}
