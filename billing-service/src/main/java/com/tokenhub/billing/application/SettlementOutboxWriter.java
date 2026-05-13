package com.tokenhub.billing.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenhub.billing.infrastructure.persistence.SettlementOutboxMapper;
import com.tokenhub.billing.infrastructure.persistence.SettlementOutboxPo;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * O-2 Outbox 写入端口（应用层服务）：在「业务事务内」追加一条 PENDING 事件，
 * 与 {@code request_orders / usage_ledger} 同事务提交。
 *
 * <p>幂等：依赖唯一键 {@code (aggregate_type, aggregate_id, event_type)}，重复写入静默吞掉。
 */
@Service
public class SettlementOutboxWriter {

  private final SettlementOutboxMapper outboxMapper;
  private final ObjectMapper objectMapper;

  public SettlementOutboxWriter(
      SettlementOutboxMapper outboxMapper,
      ObjectMapper objectMapper
  ) {
    this.outboxMapper = outboxMapper;
    this.objectMapper = objectMapper;
  }

  /**
   * 必须由调用方的 {@code @Transactional} 包裹；本方法以 {@code REQUIRED} 复用同事务，
   * 保证「业务写入 + outbox 写入」原子。
   */
  @Transactional(propagation = Propagation.REQUIRED)
  public void append(
      String aggregateType,
      String aggregateId,
      String eventType,
      Map<String, ?> payload
  ) {
    if (aggregateId == null || aggregateId.isBlank() || eventType == null || eventType.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "outbox 事件参数缺失");
    }
    SettlementOutboxPo exists = outboxMapper.selectOne(
        new LambdaQueryWrapper<SettlementOutboxPo>()
            .eq(SettlementOutboxPo::getAggregateType, aggregateType)
            .eq(SettlementOutboxPo::getAggregateId, aggregateId)
            .eq(SettlementOutboxPo::getEventType, eventType)
            .last("LIMIT 1")
    );
    if (exists != null) {
      return;
    }
    SettlementOutboxPo po = new SettlementOutboxPo();
    po.setAggregateType(aggregateType);
    po.setAggregateId(aggregateId);
    po.setEventType(eventType);
    String json;
    try {
      json = objectMapper.writeValueAsString(payload);
    } catch (Exception ex) {
      json = "{}";
    }
    po.setPayloadJson(json);
    po.setStatus("PENDING");
    po.setAttempts(0);
    try {
      outboxMapper.insert(po);
    } catch (DuplicateKeyException ignored) {
      // 并发下另一线程已写入，幂等吞掉
    }
  }
}
