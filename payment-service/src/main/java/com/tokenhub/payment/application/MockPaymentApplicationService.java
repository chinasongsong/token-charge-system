package com.tokenhub.payment.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MockPaymentApplicationService {

  private final PaymentExecutionService paymentExecutionService;

  public MockPaymentApplicationService(PaymentExecutionService paymentExecutionService) {
    this.paymentExecutionService = paymentExecutionService;
  }

  public record MockRechargeResult(String orderNo, long amount, String currency, String status) {}

  @Transactional
  public MockRechargeResult mockRecharge(long userId, long amount) {
    PaymentExecutionService.PaidOrder o = paymentExecutionService.payRecharge(userId, amount, "mock", "pay_");
    return new MockRechargeResult(o.orderNo(), o.amount(), o.currency(), o.status());
  }

  /** 创建 INIT 订单，等待签名校验回调入账（与 mockRecharge 即时到账区分）。 */
  @Transactional
  public MockRechargeResult mockCheckout(long userId, long amount) {
    PaymentExecutionService.PaidOrder o = paymentExecutionService.createPendingOrder(userId, amount, "mock", "pay_");
    return new MockRechargeResult(o.orderNo(), o.amount(), o.currency(), o.status());
  }
}
