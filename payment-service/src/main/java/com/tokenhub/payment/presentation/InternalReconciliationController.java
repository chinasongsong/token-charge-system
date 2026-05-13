package com.tokenhub.payment.presentation;

import com.tokenhub.common.core.api.ApiResponse;
import com.tokenhub.payment.application.ChannelReconciliationApplicationService;
import com.tokenhub.payment.presentation.dto.ReconciliationImportRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O-4 内部对账接口：导入渠道明细 + 查询比对汇总。
 *
 * <p>调账落地（adjustment_tickets 表）在 M2 接入 ops-console 审批 + 调用 billing 内部 credit/debit 时补齐。
 */
@RestController
@RequestMapping("/internal/payments/reconciliation")
@Validated
public class InternalReconciliationController {

  private final ChannelReconciliationApplicationService reconciliationService;

  public InternalReconciliationController(ChannelReconciliationApplicationService reconciliationService) {
    this.reconciliationService = reconciliationService;
  }

  @PostMapping("/batches")
  public ApiResponse<ChannelReconciliationApplicationService.ImportResult> importBatch(
      @Valid @RequestBody ReconciliationImportRequest request
  ) {
    return ApiResponse.ok(
        reconciliationService.importBatch(
            request.channel(),
            request.billDate(),
            request.sourceName(),
            request.csv()
        )
    );
  }

  @GetMapping("/batches/{id}")
  public ApiResponse<ChannelReconciliationApplicationService.BatchSummary> getBatch(@PathVariable("id") long id) {
    return ApiResponse.ok(reconciliationService.getBatchSummary(id));
  }
}
