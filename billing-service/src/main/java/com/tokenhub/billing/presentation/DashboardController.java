package com.tokenhub.billing.presentation;

import com.tokenhub.billing.application.BillingQueryApplicationService;
import com.tokenhub.billing.domain.auth.BillingAuthConstants;
import com.tokenhub.common.core.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

  private final BillingQueryApplicationService billingQueryApplicationService;

  public DashboardController(BillingQueryApplicationService billingQueryApplicationService) {
    this.billingQueryApplicationService = billingQueryApplicationService;
  }

  @GetMapping("/summary")
  public ApiResponse<BillingQueryApplicationService.DashboardSummary> summary(HttpServletRequest http) {
    Long userId = (Long) http.getAttribute(BillingAuthConstants.REQUEST_USER_ID);
    return ApiResponse.ok(billingQueryApplicationService.dashboardSummary(userId));
  }
}
