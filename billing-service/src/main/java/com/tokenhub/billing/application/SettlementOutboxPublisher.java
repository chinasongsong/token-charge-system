package com.tokenhub.billing.application;

/**
 * O-2 发布端口：将 settlement_outbox 中的一条记录投递到外部消息系统。
 *
 * <p>首版默认实现：仅日志 + 翻转为 SENT；后续 P7 接 RabbitMQ 时替换为发布到 exchange。
 * 失败请抛运行时异常，由 {@code SettlementOutboxScheduler} 计入 attempts 并设置下次重试时间。
 */
public interface SettlementOutboxPublisher {

  void publish(String aggregateType, String aggregateId, String eventType, String payloadJson);
}
