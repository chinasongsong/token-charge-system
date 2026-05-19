package com.tokenhub.billing.presentation;

import com.tokenhub.billing.application.AccountBalanceApplicationService;
import com.tokenhub.billing.application.BalanceReservationApplicationService;
import com.tokenhub.billing.application.BillingSettlementApplicationService;
import com.tokenhub.billing.application.BillingSettlementFacade;
import com.tokenhub.billing.presentation.dto.CreditRequest;
import com.tokenhub.billing.presentation.dto.PreflightRequest;
import com.tokenhub.billing.presentation.dto.ReservationCommitRequest;
import com.tokenhub.billing.presentation.dto.ReservationReleaseRequest;
import com.tokenhub.billing.presentation.dto.ReserveRequest;
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
  private final BalanceReservationApplicationService balanceReservationApplicationService;

  public InternalBillingController(
      AccountBalanceApplicationService accountBalanceApplicationService,
      BillingSettlementFacade billingSettlementFacade,
      BalanceReservationApplicationService balanceReservationApplicationService
  ) {
    this.accountBalanceApplicationService = accountBalanceApplicationService;
    this.billingSettlementFacade = billingSettlementFacade;
    this.balanceReservationApplicationService = balanceReservationApplicationService;
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
            request.outputTokens(),
            request.idempotencyKey(),
            request.idempotencySource()
        )
    );
  }

  @PostMapping("/reserve")
  public BalanceReservationApplicationService.ReservationView reserve(@Valid @RequestBody ReserveRequest request) {
    return balanceReservationApplicationService.reserve(request.traceId(), request.userId(), request.amount());
  }

  @PostMapping("/reservations/commit")
  public BalanceReservationApplicationService.ReservationView commitReservation(
      @Valid @RequestBody ReservationCommitRequest request
  ) {
    return balanceReservationApplicationService.commit(request.traceId(), request.committedAmount());
  }

  @PostMapping("/reservations/release")
  public BalanceReservationApplicationService.ReservationView releaseReservation(
      @Valid @RequestBody ReservationReleaseRequest request
  ) {
    return balanceReservationApplicationService.release(request.traceId());
  }
}
