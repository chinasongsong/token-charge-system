package com.tokenhub.billing.infrastructure.outbox;

import com.tokenhub.billing.application.SettlementOutboxPublisher;
import com.tokenhub.billing.infrastructure.persistence.SettlementOutboxMapper;
import com.tokenhub.billing.infrastructure.persistence.SettlementOutboxPo;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * O-2 Outbox 轮询发布器：定时拉取 PENDING 事件，调用 {@link SettlementOutboxPublisher} 投递。
 *
 * <p>默认 {@code tokenhub.billing.outbox.enabled=false}（与 P7 RabbitMQ 同步开启）。
 */
@Component
public class SettlementOutboxScheduler {

  private static final Logger log = LoggerFactory.getLogger(SettlementOutboxScheduler.class);

  private final SettlementOutboxMapper outboxMapper;
  private final SettlementOutboxPublisher publisher;

  @Value("${tokenhub.billing.outbox.enabled:false}")
  private boolean enabled;

  @Value("${tokenhub.billing.outbox.batch-size:50}")
  private int batchSize;

  @Value("${tokenhub.billing.outbox.max-attempts:8}")
  private int maxAttempts;

  @Value("${tokenhub.billing.outbox.backoff-base-seconds:30}")
  private int backoffBaseSeconds;

  public SettlementOutboxScheduler(
      SettlementOutboxMapper outboxMapper,
      SettlementOutboxPublisher publisher
  ) {
    this.outboxMapper = outboxMapper;
    this.publisher = publisher;
  }

  @Scheduled(fixedDelayString = "${tokenhub.billing.outbox.fixed-delay-ms:2000}")
  public void drain() {
    if (!enabled) {
      return;
    }
    List<SettlementOutboxPo> batch = outboxMapper.claimPendingBatch(Math.max(1, batchSize));
    if (batch.isEmpty()) {
      return;
    }
    for (SettlementOutboxPo row : batch) {
      try {
        publisher.publish(
            row.getAggregateType(),
            row.getAggregateId(),
            row.getEventType(),
            row.getPayloadJson()
        );
        row.setStatus("SENT");
        row.setLastError(null);
        outboxMapper.updateById(row);
      } catch (RuntimeException ex) {
        int attempts = (row.getAttempts() == null ? 0 : row.getAttempts()) + 1;
        row.setAttempts(attempts);
        row.setLastError(truncate(ex.getMessage(), 1000));
        if (attempts >= Math.max(1, maxAttempts)) {
          row.setStatus("FAILED");
          log.error("settlement-outbox give up after {} attempts: id={}", attempts, row.getId(), ex);
        } else {
          long backoff = (long) backoffBaseSeconds << Math.min(8, attempts - 1);
          row.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoff));
          log.warn(
              "settlement-outbox publish failed: id={}, attempts={}, retryInSec={}",
              row.getId(),
              attempts,
              backoff,
              ex
          );
        }
        outboxMapper.updateById(row);
      }
    }
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return null;
    }
    return s.length() <= max ? s : s.substring(0, max);
  }
}
