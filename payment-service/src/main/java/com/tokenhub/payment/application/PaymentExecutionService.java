package com.tokenhub.payment.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.payment.infrastructure.billing.BillingCreditClient;
import com.tokenhub.payment.infrastructure.persistence.PaymentOrderMapper;
import com.tokenhub.payment.infrastructure.persistence.PaymentOrderPo;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentExecutionService {

  private final PaymentOrderMapper paymentOrderMapper;
  private final BillingCreditClient billingCreditClient;

  public PaymentExecutionService(
      PaymentOrderMapper paymentOrderMapper,
      BillingCreditClient billingCreditClient
  ) {
    this.paymentOrderMapper = paymentOrderMapper;
    this.billingCreditClient = billingCreditClient;
  }

  public record PaidOrder(String orderNo, long amount, String currency, String status) {}

  /**
   * 仅创建 INIT 订单，入账由回调或补偿任务触发。
   */
  @Transactional
  public PaidOrder createPendingOrder(long userId, long amount, String channel, String orderPrefix) {
    String orderNo = orderPrefix + UUID.randomUUID().toString().replace("-", "");
    PaymentOrderPo row = new PaymentOrderPo();
    row.setUserId(userId);
    row.setOrderNo(orderNo);
    row.setChannel(channel != null ? channel : "mock");
    row.setAmount(amount);
    row.setCurrency("TOKEN");
    row.setStatus("INIT");
    paymentOrderMapper.insert(row);
    return new PaidOrder(orderNo, amount, "TOKEN", "INIT");
  }

  /**
   * 同步充值（立即调 billing 入账），保持兼容。
   */
  @Transactional
  public PaidOrder payRecharge(long userId, long amount, String channel, String orderPrefix) {
    String orderNo = orderPrefix + UUID.randomUUID().toString().replace("-", "");
    PaymentOrderPo row = new PaymentOrderPo();
    row.setUserId(userId);
    row.setOrderNo(orderNo);
    row.setChannel(channel != null ? channel : "mock");
    row.setAmount(amount);
    row.setCurrency("TOKEN");
    row.setStatus("INIT");
    paymentOrderMapper.insert(row);
    billingCreditClient.creditBalance(userId, amount, orderNo);
    row.setStatus("PAID");
    paymentOrderMapper.updateById(row);
    return new PaidOrder(orderNo, amount, "TOKEN", "PAID");
  }

  /** 供回调拉单：按订单号加载（不校验用户，调用方已验签）。 */
  public PaymentOrderPo findByOrderNo(String orderNo) {
    return paymentOrderMapper.selectOne(
        new LambdaQueryWrapper<PaymentOrderPo>().eq(PaymentOrderPo::getOrderNo, orderNo)
    );
  }

  /**
   * Mock/三方回调：幂等入账。sourceref 与 billing credit 一致，重复调 billing 侧亦幂等。
   */
  @Transactional
  public PaidOrder completePendingFromCallback(String orderNo) {
    PaymentOrderPo row =
        paymentOrderMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrderPo>().eq(PaymentOrderPo::getOrderNo, orderNo)
        );
    if (row == null) {
      return null;
    }
    if ("PAID".equals(row.getStatus())) {
      return new PaidOrder(row.getOrderNo(), row.getAmount(), row.getCurrency(), "PAID");
    }
    if (!"INIT".equals(row.getStatus())) {
      return new PaidOrder(row.getOrderNo(), row.getAmount(), row.getCurrency(), row.getStatus());
    }
    billingCreditClient.creditBalance(row.getUserId(), row.getAmount(), row.getOrderNo());
    row.setStatus("PAID");
    paymentOrderMapper.updateById(row);
    return new PaidOrder(row.getOrderNo(), row.getAmount(), row.getCurrency(), "PAID");
  }
}
