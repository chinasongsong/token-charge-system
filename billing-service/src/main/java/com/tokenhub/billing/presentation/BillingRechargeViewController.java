package com.tokenhub.billing.presentation;

import com.tokenhub.billing.domain.auth.BillingAuthConstants;
import com.tokenhub.billing.infrastructure.payment.PaymentRechargeClient;
import com.tokenhub.billing.presentation.dto.BillingRechargeRequest;
import com.tokenhub.common.core.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing")
@Validated
public class BillingRechargeViewController {

  private final PaymentRechargeClient paymentRechargeClient;

  public BillingRechargeViewController(PaymentRechargeClient paymentRechargeClient) {
    this.paymentRechargeClient = paymentRechargeClient;
  }

  @PostMapping("/recharge")
  public ApiResponse<PaymentRechargeClient.PaidOrder> recharge(
      @Valid @RequestBody BillingRechargeRequest request,
      HttpServletRequest http
  ) {
    long userId = (Long) http.getAttribute(BillingAuthConstants.REQUEST_USER_ID);
    PaymentRechargeClient.PaidOrder o = paymentRechargeClient.rechargeViaPayment(userId, request.amount());
    return ApiResponse.ok(o);
  }
}
