package com.tokenhub.billing.presentation;

import com.tokenhub.billing.application.CatalogAndSubscriptionApplicationService;
import com.tokenhub.billing.infrastructure.persistence.PricingPlanPo;
import com.tokenhub.common.core.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing/plans")
public class BillingPlanViewController {

  private final CatalogAndSubscriptionApplicationService catalogAndSubscriptionApplicationService;

  public BillingPlanViewController(CatalogAndSubscriptionApplicationService catalogAndSubscriptionApplicationService) {
    this.catalogAndSubscriptionApplicationService = catalogAndSubscriptionApplicationService;
  }

  @GetMapping
  public ApiResponse<List<PricingPlanPo>> list() {
    return ApiResponse.ok(catalogAndSubscriptionApplicationService.listActivePlans());
  }
}
