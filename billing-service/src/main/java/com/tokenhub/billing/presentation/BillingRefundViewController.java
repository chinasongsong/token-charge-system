package com.tokenhub.billing.presentation;

import com.tokenhub.billing.application.RefundApplicationService;
import com.tokenhub.billing.domain.auth.BillingAuthConstants;
import com.tokenhub.billing.infrastructure.persistence.RefundRequestPo;
import com.tokenhub.billing.presentation.dto.RefundApplyRequest;
import com.tokenhub.common.core.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing/refund")
@Validated
public class BillingRefundViewController {

  private final RefundApplicationService refundApplicationService;

  public BillingRefundViewController(RefundApplicationService refundApplicationService) {
    this.refundApplicationService = refundApplicationService;
  }

  @PostMapping("/apply")
  public ApiResponse<RefundRequestPo> apply(
      @Valid @RequestBody RefundApplyRequest request,
      HttpServletRequest http
  ) {
    long userId = (Long) http.getAttribute(BillingAuthConstants.REQUEST_USER_ID);
    RefundRequestPo r = refundApplicationService.apply(
        userId,
        request.orderNo(),
        request.amount(),
        request.reason() != null ? request.reason() : ""
    );
    return ApiResponse.ok(r);
  }
}
