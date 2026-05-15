package com.tokenhub.payment.presentation;

import com.tokenhub.common.core.api.ApiResponse;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.payment.application.ChannelReconcileApplicationService;
import com.tokenhub.payment.application.PaymentExecutionService;
import com.tokenhub.payment.infrastructure.redis.PaymentCallbackOrderLock;
import com.tokenhub.payment.presentation.dto.InternalRechargeRequest;
import com.tokenhub.payment.presentation.dto.InternalRetryCreditRequest;
import jakarta.validation.Valid;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payments")
@Validated
public class InternalPaymentController {

  private final PaymentExecutionService paymentExecutionService;
  private final PaymentCallbackOrderLock callbackOrderLock;
  private final ChannelReconcileApplicationService channelReconcileApplicationService;
  private final ConcurrentHashMap<String, PaymentExecutionService.PaidOrder> idem = new ConcurrentHashMap<>();

  public InternalPaymentController(
      PaymentExecutionService paymentExecutionService,
      PaymentCallbackOrderLock callbackOrderLock,
      ChannelReconcileApplicationService channelReconcileApplicationService
  ) {
    this.paymentExecutionService = paymentExecutionService;
    this.callbackOrderLock = callbackOrderLock;
    this.channelReconcileApplicationService = channelReconcileApplicationService;
  }

  @PostMapping("/recharge")
  public ApiResponse<PaymentExecutionService.PaidOrder> recharge(@Valid @RequestBody InternalRechargeRequest req) {
    if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
      String k = req.idempotencyKey().trim();
      return ApiResponse.ok(
          idem.computeIfAbsent(
              k,
              ignored -> paymentExecutionService.payRecharge(req.userId(), req.amount(), "internal", "bill_")
          )
      );
    }
    return ApiResponse.ok(paymentExecutionService.payRecharge(req.userId(), req.amount(), "internal", "bill_"));
  }

  /**
   * 在运营/自动化已确认渠道侧「已支付」后，重试幂等入账（与回调同锁、同 billing sourceRef）。
   * 禁止对未支付订单滥用，否则会造成资金风险。
   */
  @PostMapping("/orders/retry-credit")
  public ApiResponse<PaymentExecutionService.PaidOrder> retryCredit(@Valid @RequestBody InternalRetryCreditRequest req) {
    String orderNo = req.orderNo().trim();
    PaymentExecutionService.PaidOrder[] out = new PaymentExecutionService.PaidOrder[1];
    callbackOrderLock.run(
        orderNo,
        () -> {
          PaymentExecutionService.PaidOrder r = paymentExecutionService.completePendingFromCallback(orderNo);
          if (r == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
          }
          out[0] = r;
        }
    );
    return ApiResponse.ok(out[0]);
  }

  /**
   * O-8：渠道查单 + 入账。仅当 {@link com.tokenhub.payment.application.ChannelQueryPort} 返回 PAID
   * 且金额一致时入账；否则抛错。与 {@code /retry-credit} 共享回调短锁，避免与回调路径并发双扣。
   */
  @PostMapping("/orders/{orderNo}/channel-reconcile")
  public ApiResponse<PaymentExecutionService.PaidOrder> channelReconcile(@PathVariable("orderNo") String orderNo) {
    return ApiResponse.ok(channelReconcileApplicationService.reconcileByOrderNo(orderNo));
  }
}
