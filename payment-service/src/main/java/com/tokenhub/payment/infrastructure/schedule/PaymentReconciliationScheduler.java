package com.tokenhub.payment.infrastructure.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.payment.infrastructure.persistence.PaymentOrderMapper;
import com.tokenhub.payment.infrastructure.persistence.PaymentOrderPo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 拉单补偿占位：打印待支付订单数量；接入真实通道后在此对账/关单。
 */
@Component
public class PaymentReconciliationScheduler {

  private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationScheduler.class);

  private final PaymentOrderMapper paymentOrderMapper;

  @Value("${tokenhub.payment.reconcile-enabled:false}")
  private boolean enabled;

  public PaymentReconciliationScheduler(PaymentOrderMapper paymentOrderMapper) {
    this.paymentOrderMapper = paymentOrderMapper;
  }

  @Scheduled(cron = "${tokenhub.payment.reconcile-cron:0 */10 * * * ?}")
  public void logPendingInitOrders() {
    if (!enabled) {
      return;
    }
    List<PaymentOrderPo> pending =
        paymentOrderMapper.selectList(
            new LambdaQueryWrapper<PaymentOrderPo>()
                .eq(PaymentOrderPo::getStatus, "INIT")
                .last("LIMIT 200")
        );
    log.info("payment pending INIT orders (sample up to 200): count={}", pending.size());
  }
}
