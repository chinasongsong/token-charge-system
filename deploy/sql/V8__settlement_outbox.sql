-- O-2: 异步账务 Outbox 表（billing-service 侧）。
-- 结算事务在写入 request_orders/usage_ledger 后，同事务写入一条 PENDING 事件；
-- 由 SettlementOutboxScheduler 轮询发布（首版 publisher 仅日志 + 状态翻转；P7 接 RabbitMQ）。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS settlement_outbox (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  aggregate_type VARCHAR(64) NOT NULL COMMENT 'e.g. request_order',
  aggregate_id VARCHAR(64) NOT NULL COMMENT 'traceId 或订单主键的字符串形式',
  event_type VARCHAR(64) NOT NULL COMMENT 'e.g. billing.settled',
  payload_json MEDIUMTEXT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|SENT|FAILED',
  attempts INT NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMP(3) NULL,
  last_error VARCHAR(1024) NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_outbox_status_next (status, next_attempt_at),
  UNIQUE KEY uk_outbox_agg_event (aggregate_type, aggregate_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
