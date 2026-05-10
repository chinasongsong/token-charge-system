package com.tokenhub.payment.presentation;

import com.tokenhub.common.core.api.ApiResponse;
import com.tokenhub.payment.application.PaymentCallbackApplicationService;
import com.tokenhub.payment.application.PaymentExecutionService;
import com.tokenhub.payment.application.dto.MockCallbackRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟第三方支付异步回调：需带 HMAC 签名，不经用户 JWT；生产替换为微信/支付宝验签逻辑。
 */
@RestController
@RequestMapping("/payments/mock")
@Validated
public class MockPaymentCallbackController {

  private final PaymentCallbackApplicationService paymentCallbackApplicationService;

  public MockPaymentCallbackController(PaymentCallbackApplicationService paymentCallbackApplicationService) {
    this.paymentCallbackApplicationService = paymentCallbackApplicationService;
  }

  @PostMapping("/callback")
  public ApiResponse<PaymentExecutionService.PaidOrder> callback(@Valid @RequestBody MockCallbackRequest body) {
    return ApiResponse.ok(paymentCallbackApplicationService.handleMockCallback(body));
  }
}
