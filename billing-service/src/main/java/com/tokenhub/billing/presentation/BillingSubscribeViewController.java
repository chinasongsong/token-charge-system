package com.tokenhub.billing.presentation;

import com.tokenhub.billing.application.CatalogAndSubscriptionApplicationService;
import com.tokenhub.billing.domain.auth.BillingAuthConstants;
import com.tokenhub.billing.infrastructure.persistence.UserSubscriptionPo;
import com.tokenhub.billing.presentation.dto.SubscribeRequest;
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
public class BillingSubscribeViewController {

  private final CatalogAndSubscriptionApplicationService catalogAndSubscriptionApplicationService;

  public BillingSubscribeViewController(
      CatalogAndSubscriptionApplicationService catalogAndSubscriptionApplicationService
  ) {
    this.catalogAndSubscriptionApplicationService = catalogAndSubscriptionApplicationService;
  }

  @PostMapping("/subscribe")
  public ApiResponse<UserSubscriptionPo> subscribe(
      @Valid @RequestBody SubscribeRequest request,
      HttpServletRequest http
  ) {
    long userId = (Long) http.getAttribute(BillingAuthConstants.REQUEST_USER_ID);
    UserSubscriptionPo s = catalogAndSubscriptionApplicationService.subscribe(userId, request.planCode().trim());
    return ApiResponse.ok(s);
  }
}
