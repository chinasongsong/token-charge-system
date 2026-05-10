package com.tokenhub.billing.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenhub.billing.infrastructure.persistence.RequestOrderMapper;
import com.tokenhub.billing.infrastructure.persistence.RequestOrderPo;
import com.tokenhub.billing.infrastructure.persistence.UsageLedgerMapper;
import com.tokenhub.billing.infrastructure.persistence.UsageLedgerPo;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingSettlementApplicationService {

  private final RequestOrderMapper requestOrderMapper;
  private final UsageLedgerMapper usageLedgerMapper;
  private final PricingService pricingService;
  private final AccountBalanceApplicationService accountBalanceApplicationService;
  private final ObjectMapper objectMapper;

  public BillingSettlementApplicationService(
      RequestOrderMapper requestOrderMapper,
      UsageLedgerMapper usageLedgerMapper,
      PricingService pricingService,
      AccountBalanceApplicationService accountBalanceApplicationService,
      ObjectMapper objectMapper
  ) {
    this.requestOrderMapper = requestOrderMapper;
    this.usageLedgerMapper = usageLedgerMapper;
    this.pricingService = pricingService;
    this.accountBalanceApplicationService = accountBalanceApplicationService;
    this.objectMapper = objectMapper;
  }

  public record SettlementCommand(
      String traceId,
      Long userId,
      Long apiKeyId,
      String providerCode,
      String modelName,
      long inputTokens,
      long outputTokens
  ) {}

  @Transactional
  public void settle(SettlementCommand cmd) {
    if (cmd.traceId() == null || cmd.traceId().isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "traceId 不能为空");
    }
    RequestOrderPo done = requestOrderMapper.selectOne(
        new LambdaQueryWrapper<RequestOrderPo>()
            .eq(RequestOrderPo::getIdempotencyKey, cmd.traceId())
    );
    if (done != null && "COMPLETED".equals(done.getBillingStatus())) {
      return;
    }

    RequestOrderPo pending = new RequestOrderPo();
    pending.setTraceId(cmd.traceId());
    pending.setApiKeyId(cmd.apiKeyId());
    pending.setUserId(cmd.userId());
    pending.setProviderCode(cmd.providerCode());
    pending.setModelName(cmd.modelName());
    pending.setBillingStatus("PENDING");
    pending.setInputTokens(cmd.inputTokens());
    pending.setOutputTokens(cmd.outputTokens());
    pending.setAmount(0L);
    pending.setIdempotencyKey(cmd.traceId());
    try {
      requestOrderMapper.insert(pending);
    } catch (DuplicateKeyException ex) {
      return;
    }

    long amount = pricingService.computeChargeMicro(
        cmd.providerCode(),
        cmd.modelName(),
        cmd.inputTokens(),
        cmd.outputTokens()
    );
    accountBalanceApplicationService.debit(cmd.userId(), amount);

    pending.setAmount(amount);
    pending.setBillingStatus("COMPLETED");
    requestOrderMapper.updateById(pending);

    UsageLedgerPo ledger = new UsageLedgerPo();
    ledger.setUserId(cmd.userId());
    ledger.setRequestOrderId(pending.getId());
    ledger.setEntryType("USAGE");
    ledger.setQuantity(amount);
    ledger.setIdempotencyKey(cmd.traceId());
    Map<String, Object> detail = new HashMap<>();
    detail.put("providerCode", cmd.providerCode());
    detail.put("modelName", cmd.modelName());
    detail.put("inputTokens", cmd.inputTokens());
    detail.put("outputTokens", cmd.outputTokens());
    try {
      ledger.setDetailJson(objectMapper.writeValueAsString(detail));
    } catch (JsonProcessingException ex) {
      ledger.setDetailJson("{}");
    }
    ledger.setRecordedAt(LocalDateTime.now());
    usageLedgerMapper.insert(ledger);
  }
}
