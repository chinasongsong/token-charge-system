package com.tokenhub.billing.presentation;

import com.tokenhub.billing.application.AccountBalanceApplicationService;
import com.tokenhub.billing.application.BillingSettlementApplicationService;
import com.tokenhub.billing.application.BillingSettlementFacade;
import com.tokenhub.billing.presentation.dto.CreditRequest;
import com.tokenhub.billing.presentation.dto.PreflightRequest;
import com.tokenhub.billing.presentation.dto.SettlementRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/billing")
@Validated
public class InternalBillingController {

  private final AccountBalanceApplicationService accountBalanceApplicationService;
  private final BillingSettlementFacade billingSettlementFacade;

  public InternalBillingController(
      AccountBalanceApplicationService accountBalanceApplicationService,
      BillingSettlementFacade billingSettlementFacade
  ) {
    this.accountBalanceApplicationService = accountBalanceApplicationService;
    this.billingSettlementFacade = billingSettlementFacade;
  }

  @PostMapping("/preflight")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void preflight(@Valid @RequestBody PreflightRequest request) {
    accountBalanceApplicationService.assertPositiveBalance(request.userId());
  }

  @PostMapping("/credit")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void credit(@Valid @RequestBody CreditRequest request) {
    accountBalanceApplicationService.creditIdempotent(
        request.userId(),
        request.amount(),
        request.sourceRef()
    );
  }

  @PostMapping("/settle")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void settle(@Valid @RequestBody SettlementRequest request) {
    billingSettlementFacade.settle(
        new BillingSettlementApplicationService.SettlementCommand(
            request.traceId(),
            request.userId(),
            request.apiKeyId(),
            request.providerCode(),
            request.modelName(),
            request.inputTokens(),
            request.outputTokens()
        )
    );
  }
}
