package com.tokenhub.billing.presentation;

import com.tokenhub.billing.application.AccountBalanceApplicationService;
import com.tokenhub.billing.domain.auth.BillingAuthConstants;
import com.tokenhub.billing.presentation.dto.MockDepositRequest;
import com.tokenhub.common.core.api.ApiResponse;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing/account")
@Validated
public class BillingAccountController {

  private final AccountBalanceApplicationService accountBalanceApplicationService;

  @Value("${tokenhub.billing.allow-mock-deposit:false}")
  private boolean allowMockDeposit;

  public BillingAccountController(AccountBalanceApplicationService accountBalanceApplicationService) {
    this.accountBalanceApplicationService = accountBalanceApplicationService;
  }

  @PostMapping("/mock-deposit")
  public ApiResponse<Void> mockDeposit(
      @Valid @RequestBody MockDepositRequest request,
      HttpServletRequest http
  ) {
    if (!allowMockDeposit) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "未开放模拟充值");
    }
    Long userId = (Long) http.getAttribute(BillingAuthConstants.REQUEST_USER_ID);
    accountBalanceApplicationService.credit(userId, request.amount());
    return ApiResponse.ok();
  }
}
