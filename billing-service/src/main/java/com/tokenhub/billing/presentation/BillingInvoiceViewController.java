package com.tokenhub.billing.presentation;

import com.tokenhub.billing.application.InvoiceApplicationService;
import com.tokenhub.billing.domain.auth.BillingAuthConstants;
import com.tokenhub.billing.infrastructure.persistence.InvoicePo;
import com.tokenhub.billing.presentation.dto.InvoiceRequest;
import com.tokenhub.common.core.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing/invoices")
@Validated
public class BillingInvoiceViewController {

  private final InvoiceApplicationService invoiceApplicationService;

  public BillingInvoiceViewController(InvoiceApplicationService invoiceApplicationService) {
    this.invoiceApplicationService = invoiceApplicationService;
  }

  @PostMapping("/request")
  public ApiResponse<InvoicePo> request(
      @Valid @RequestBody InvoiceRequest request,
      HttpServletRequest http
  ) {
    long userId = (Long) http.getAttribute(BillingAuthConstants.REQUEST_USER_ID);
    String cur = request.currency() != null ? request.currency() : "CNY";
    InvoicePo inv = invoiceApplicationService.issuePlaceholder(userId, request.orderRef(), request.amount(), cur);
    return ApiResponse.ok(inv);
  }
}
