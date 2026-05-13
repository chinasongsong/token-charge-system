package com.tokenhub.billing.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * O-1：在 {@link BillingSettlementApplicationService#settle} 外围加可选「用户维度锁」。
 *
 * <p>放在 application 层的薄包装，避免在 {@code @Transactional} 内持锁阻塞数据库连接；
 * 锁在事务边界之外获取并释放：先拿锁，再进入事务方法。
 *
 * <p>默认通过 {@code tokenhub.billing.balance-lock.enabled=false} 关闭以保持原有行为。
 */
@Service
public class BillingSettlementFacade {

  private final BillingSettlementApplicationService billingSettlementApplicationService;
  private final BalanceLock balanceLock;

  @Value("${tokenhub.billing.balance-lock.enabled:false}")
  private boolean lockEnabled;

  public BillingSettlementFacade(
      BillingSettlementApplicationService billingSettlementApplicationService,
      BalanceLock balanceLock
  ) {
    this.billingSettlementApplicationService = billingSettlementApplicationService;
    this.balanceLock = balanceLock;
  }

  public void settle(BillingSettlementApplicationService.SettlementCommand cmd) {
    if (!lockEnabled || cmd == null || cmd.userId() == null) {
      billingSettlementApplicationService.settle(cmd);
      return;
    }
    balanceLock.runForUser(cmd.userId(), () -> billingSettlementApplicationService.settle(cmd));
  }
}
