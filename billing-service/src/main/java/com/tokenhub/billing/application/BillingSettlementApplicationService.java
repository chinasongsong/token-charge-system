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
  private final SettlementOutboxWriter settlementOutboxWriter;

  public BillingSettlementApplicationService(
      RequestOrderMapper requestOrderMapper,
      UsageLedgerMapper usageLedgerMapper,
      PricingService pricingService,
      AccountBalanceApplicationService accountBalanceApplicationService,
      ObjectMapper objectMapper,
      SettlementOutboxWriter settlementOutboxWriter
  ) {
    this.requestOrderMapper = requestOrderMapper;
    this.usageLedgerMapper = usageLedgerMapper;
    this.pricingService = pricingService;
    this.accountBalanceApplicationService = accountBalanceApplicationService;
    this.objectMapper = objectMapper;
    this.settlementOutboxWriter = settlementOutboxWriter;
  }

  public record SettlementCommand(
      String traceId,
      Long userId,
      Long apiKeyId,
      String providerCode,
      String modelName,
      long inputTokens,
      long outputTokens,
      String idempotencyKey,      // 复合幂等键 userId:apiKeyId:clientKey
      String idempotencySource    // CLIENT | TRACE_ID_FALLBACK
  ) {}

  @Transactional
  public void settle(SettlementCommand cmd) {
    if (cmd.traceId() == null || cmd.traceId().isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "traceId 不能为空");
    }

    // O-10：使用复合幂等键（若提供）或回退 traceId
    String effectiveIdempotencyKey = (cmd.idempotencyKey() != null && !cmd.idempotencyKey().isBlank())
        ? cmd.idempotencyKey()
        : cmd.traceId();
    String idempotencySource = (cmd.idempotencySource() != null && !cmd.idempotencySource().isBlank())
        ? cmd.idempotencySource()
        : "TRACE_ID_FALLBACK";

    RequestOrderPo done = requestOrderMapper.selectOne(
        new LambdaQueryWrapper<RequestOrderPo>()
            .eq(RequestOrderPo::getIdempotencyKey, effectiveIdempotencyKey)
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
    pending.setIdempotencyKey(effectiveIdempotencyKey);
    pending.setIdempotencySource(idempotencySource);
    // 若客户端提供幂等键，设置过期时间（24小时后）
    if ("CLIENT".equals(idempotencySource)) {
      pending.setIdempotencyExpiresAt(LocalDateTime.now().plusHours(24));
    }
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
    ledger.setIdempotencyKey(effectiveIdempotencyKey);
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

    // O-2 Outbox：与上述写入同事务追加 PENDING 事件，由 SettlementOutboxScheduler 异步发布。
    Map<String, Object> event = new HashMap<>();
    event.put("traceId", cmd.traceId());
    event.put("userId", cmd.userId());
    event.put("apiKeyId", cmd.apiKeyId());
    event.put("providerCode", cmd.providerCode());
    event.put("modelName", cmd.modelName());
    event.put("inputTokens", cmd.inputTokens());
    event.put("outputTokens", cmd.outputTokens());
    event.put("amount", amount);
    event.put("requestOrderId", pending.getId());
    settlementOutboxWriter.append(
        "request_order",
        effectiveIdempotencyKey,
        "billing.settled",
        event
    );
  }
}
