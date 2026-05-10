package com.tokenhub.payment.application;

import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.payment.infrastructure.redis.PaymentCallbackOrderLock;
import com.tokenhub.payment.application.dto.MockCallbackRequest;
import org.springframework.stereotype.Service;

@Service
public class PaymentCallbackApplicationService {

  private final MockCallbackSignatureVerifier signatureVerifier;
  private final PaymentExecutionService paymentExecutionService;
  private final PaymentCallbackOrderLock callbackOrderLock;

  public PaymentCallbackApplicationService(
      MockCallbackSignatureVerifier signatureVerifier,
      PaymentExecutionService paymentExecutionService,
      PaymentCallbackOrderLock callbackOrderLock
  ) {
    this.signatureVerifier = signatureVerifier;
    this.paymentExecutionService = paymentExecutionService;
    this.callbackOrderLock = callbackOrderLock;
  }

  public PaymentExecutionService.PaidOrder handleMockCallback(MockCallbackRequest request) {
    signatureVerifier.verify(request);
    assertOrderMatches(request);
    PaymentExecutionService.PaidOrder[] out = new PaymentExecutionService.PaidOrder[1];
    callbackOrderLock.run(request.orderNo(), () -> out[0] = paymentExecutionService.completePendingFromCallback(request.orderNo()));
    return out[0];
  }

  private void assertOrderMatches(MockCallbackRequest request) {
    var row = paymentExecutionService.findByOrderNo(request.orderNo());
    if (row == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
    }
    if (!row.getUserId().equals(request.userId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "用户与订单不匹配");
    }
    if (!row.getAmount().equals(request.amount())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "金额与订单不匹配");
    }
  }
}
