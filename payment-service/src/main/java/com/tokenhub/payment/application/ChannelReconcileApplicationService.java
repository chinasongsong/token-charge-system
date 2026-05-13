package com.tokenhub.payment.application;

import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.payment.infrastructure.persistence.PaymentOrderPo;
import com.tokenhub.payment.infrastructure.redis.PaymentCallbackOrderLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * O-8 渠道查单 + retry-credit 串联：
 *
 * <ol>
 *   <li>查本地订单：不存在则 404；已 PAID 直接返回；非 INIT 视为终态不动；
 *   <li>调 {@link ChannelQueryPort#query}：仅当返回 PAID 且金额匹配时才允许入账；
 *   <li>持有订单回调短锁（与 mock 回调路径共享同一把锁），调用 {@link PaymentExecutionService#completePendingFromCallback}。
 * </ol>
 *
 * <p><b>安全</b>：UNKNOWN/UNPAID/AMOUNT_MISMATCH 均拒绝入账并抛业务异常，保证「确认已支付才入账」原则。
 */
@Service
public class ChannelReconcileApplicationService {

  private static final Logger log = LoggerFactory.getLogger(ChannelReconcileApplicationService.class);

  private final PaymentExecutionService paymentExecutionService;
  private final ChannelQueryPort channelQueryPort;
  private final PaymentCallbackOrderLock callbackOrderLock;

  public ChannelReconcileApplicationService(
      PaymentExecutionService paymentExecutionService,
      ChannelQueryPort channelQueryPort,
      PaymentCallbackOrderLock callbackOrderLock
  ) {
    this.paymentExecutionService = paymentExecutionService;
    this.channelQueryPort = channelQueryPort;
    this.callbackOrderLock = callbackOrderLock;
  }

  public PaymentExecutionService.PaidOrder reconcileByOrderNo(String orderNo) {
    if (orderNo == null || orderNo.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "orderNo 不能为空");
    }
    PaymentOrderPo local = paymentExecutionService.findByOrderNo(orderNo);
    if (local == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
    }
    if ("PAID".equalsIgnoreCase(local.getStatus())) {
      return new PaymentExecutionService.PaidOrder(local.getOrderNo(), local.getAmount(), local.getCurrency(), "PAID");
    }
    if (!"INIT".equalsIgnoreCase(local.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "订单非 INIT 终态，不处理: " + local.getStatus());
    }

    ChannelQueryPort.QueryResult q = channelQueryPort.query(local.getChannel(), orderNo);
    if (q == null || q.status() != ChannelQueryPort.ChannelStatus.PAID) {
      throw new BusinessException(
          ErrorCode.CONFLICT,
          "渠道未返回已支付，拒绝入账: " + (q == null ? "UNKNOWN" : q.status())
      );
    }
    if (q.channelAmount() != null && !q.channelAmount().equals(local.getAmount())) {
      log.warn(
          "channel-reconcile amount mismatch: orderNo={}, local={}, channel={}",
          orderNo,
          local.getAmount(),
          q.channelAmount()
      );
      throw new BusinessException(ErrorCode.CONFLICT, "渠道金额与本地不一致，禁止入账");
    }

    PaymentExecutionService.PaidOrder[] out = new PaymentExecutionService.PaidOrder[1];
    callbackOrderLock.run(orderNo, () -> out[0] = paymentExecutionService.completePendingFromCallback(orderNo));
    if (out[0] == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "订单在锁内消失");
    }
    log.info("channel-reconcile credited: orderNo={}, status={}, raw={}", orderNo, out[0].status(), q.raw());
    return out[0];
  }
}
