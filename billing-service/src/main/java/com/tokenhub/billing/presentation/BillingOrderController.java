package com.tokenhub.billing.presentation;

import com.tokenhub.billing.application.BillingQueryApplicationService;
import com.tokenhub.billing.domain.auth.BillingAuthConstants;
import com.tokenhub.billing.infrastructure.persistence.RequestOrderPo;
import com.tokenhub.common.core.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing/orders")
public class BillingOrderController {

  private final BillingQueryApplicationService billingQueryApplicationService;

  public BillingOrderController(BillingQueryApplicationService billingQueryApplicationService) {
    this.billingQueryApplicationService = billingQueryApplicationService;
  }

  @GetMapping
  public ApiResponse<List<RequestOrderPo>> list(
      @RequestParam(defaultValue = "50") int limit,
      HttpServletRequest http
  ) {
    Long userId = (Long) http.getAttribute(BillingAuthConstants.REQUEST_USER_ID);
    return ApiResponse.ok(billingQueryApplicationService.listOrders(userId, limit));
  }
}
