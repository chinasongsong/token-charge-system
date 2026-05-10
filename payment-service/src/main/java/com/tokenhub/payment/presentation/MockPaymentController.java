package com.tokenhub.payment.presentation;

import com.tokenhub.common.core.api.ApiResponse;
import com.tokenhub.payment.application.MockPaymentApplicationService;
import com.tokenhub.payment.domain.auth.PaymentAuthConstants;
import com.tokenhub.payment.presentation.dto.MockRechargeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/mock")
@Validated
public class MockPaymentController {

  private final MockPaymentApplicationService mockPaymentApplicationService;

  public MockPaymentController(MockPaymentApplicationService mockPaymentApplicationService) {
    this.mockPaymentApplicationService = mockPaymentApplicationService;
  }

  @PostMapping("/recharge")
  public ApiResponse<MockPaymentApplicationService.MockRechargeResult> recharge(
      @Valid @RequestBody MockRechargeRequest request,
      HttpServletRequest http
  ) {
    Long userId = (Long) http.getAttribute(PaymentAuthConstants.REQUEST_USER_ID);
    return ApiResponse.ok(mockPaymentApplicationService.mockRecharge(userId, request.amount()));
  }

  @PostMapping("/checkout")
  public ApiResponse<MockPaymentApplicationService.MockRechargeResult> checkout(
      @Valid @RequestBody MockRechargeRequest request,
      HttpServletRequest http
  ) {
    Long userId = (Long) http.getAttribute(PaymentAuthConstants.REQUEST_USER_ID);
    return ApiResponse.ok(mockPaymentApplicationService.mockCheckout(userId, request.amount()));
  }
}
