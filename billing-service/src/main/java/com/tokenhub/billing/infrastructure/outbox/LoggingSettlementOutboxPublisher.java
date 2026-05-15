package com.tokenhub.billing.infrastructure.outbox;

import com.tokenhub.billing.application.SettlementOutboxPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * O-2 默认 Publisher：仅打印结构化日志，不接入真实 MQ；P7 将由 RabbitMQ 实现替代。
 *
 * <p>{@link ConditionalOnMissingBean} 保证下游模块/未来扩展可通过提供自己的
 * {@link SettlementOutboxPublisher} Bean 覆盖默认实现。
 */
@Component
@Configuration
@ConditionalOnMissingBean(SettlementOutboxPublisher.class)
public class LoggingSettlementOutboxPublisher implements SettlementOutboxPublisher {

  private static final Logger log = LoggerFactory.getLogger(LoggingSettlementOutboxPublisher.class);

  @Override
  public void publish(String aggregateType, String aggregateId, String eventType, String payloadJson) {
    log.info(
        "settlement-outbox publish (default logging): aggregateType={}, aggregateId={}, eventType={}, payload={}",
        aggregateType,
        aggregateId,
        eventType,
        payloadJson
    );
  }
}
