-- O-4: 渠道对账与调账闭环（payment-service 侧）。
-- 批次：一次导入对应一个渠道 + 一个账期；行：渠道明细 + 比对结果；调账工单：长短款审批与处理。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS channel_reconciliation_batches (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  channel VARCHAR(32) NOT NULL COMMENT 'mock|wechat|alipay',
  bill_date DATE NOT NULL COMMENT '渠道账期日',
  source_name VARCHAR(255) NOT NULL COMMENT '导入文件名或来源标识',
  total_lines INT NOT NULL DEFAULT 0,
  matched_lines INT NOT NULL DEFAULT 0,
  mismatched_lines INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'IMPORTED' COMMENT 'IMPORTED|RECONCILED|CLOSED',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_batch_channel_date_name (channel, bill_date, source_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS channel_reconciliation_lines (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  channel_order_no VARCHAR(128) NOT NULL,
  local_order_no VARCHAR(64) NULL,
  user_id BIGINT NULL,
  channel_amount BIGINT NOT NULL,
  local_amount BIGINT NULL,
  channel_status VARCHAR(32) NOT NULL,
  local_status VARCHAR(32) NULL,
  diff_kind VARCHAR(32) NOT NULL DEFAULT 'MATCHED'
    COMMENT 'MATCHED|AMOUNT_MISMATCH|MISSING_LOCAL|LOCAL_INIT|LOCAL_OTHER',
  paid_at TIMESTAMP(3) NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_lines_batch (batch_id),
  KEY idx_lines_diff (diff_kind),
  CONSTRAINT fk_lines_batch FOREIGN KEY (batch_id) REFERENCES channel_reconciliation_batches (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS adjustment_tickets (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  ticket_no VARCHAR(64) NOT NULL UNIQUE,
  batch_id BIGINT NULL,
  line_id BIGINT NULL,
  user_id BIGINT NULL,
  kind VARCHAR(32) NOT NULL COMMENT 'CHANNEL_LONG|CHANNEL_SHORT|MANUAL',
  amount BIGINT NOT NULL,
  reason VARCHAR(1024) NULL,
  source_ref VARCHAR(128) NOT NULL COMMENT '入账幂等键，建议 adj:{ticket_no}',
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN|APPROVED|APPLIED|REJECTED',
  applied_at TIMESTAMP(3) NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_tickets_status (status),
  KEY idx_tickets_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
