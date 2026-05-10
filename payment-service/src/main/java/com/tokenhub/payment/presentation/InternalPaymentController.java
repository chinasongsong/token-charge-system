package com.tokenhub.payment.presentation;

import com.tokenhub.common.core.api.ApiResponse;
import com.tokenhub.payment.application.PaymentExecutionService;
import com.tokenhub.payment.presentation.dto.InternalRechargeRequest;
import jakarta.validation.Valid;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/payments")
@Validated
public class InternalPaymentController {

  private final PaymentExecutionService paymentExecutionService;
  private final ConcurrentHashMap<String, PaymentExecutionService.PaidOrder> idem = new ConcurrentHashMap<>();

  public InternalPaymentController(PaymentExecutionService paymentExecutionService) {
    this.paymentExecutionService = paymentExecutionService;
  }

  @PostMapping("/recharge")
  public ApiResponse<PaymentExecutionService.PaidOrder> recharge(@Valid @RequestBody InternalRechargeRequest req) {
    if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
      String k = req.idempotencyKey().trim();
      return ApiResponse.ok(
          idem.computeIfAbsent(
              k,
              ignored -> paymentExecutionService.payRecharge(req.userId(), req.amount(), "internal", "bill_")
          )
      );
    }
    return ApiResponse.ok(paymentExecutionService.payRecharge(req.userId(), req.amount(), "internal", "bill_"));
  }
}
