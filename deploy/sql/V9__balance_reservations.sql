-- O-3: 预占额度表（billing-service 侧）。
-- 网关或 adapter 在调用模型前 reserve 预估上限额度；调用结束后按实际用量 commit 或异常 release。
-- 余额可用量 = account_balance.balance - SUM(RESERVED.amount, 未过期)。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS balance_reservations (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  trace_id VARCHAR(191) NOT NULL,
  user_id BIGINT NOT NULL,
  amount BIGINT NOT NULL COMMENT '预占金额，扣费单位',
  status VARCHAR(16) NOT NULL DEFAULT 'RESERVED' COMMENT 'RESERVED|COMMITTED|RELEASED|EXPIRED',
  committed_amount BIGINT NULL COMMENT 'commit 时实际扣费金额',
  expires_at TIMESTAMP(3) NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_reservations_trace (trace_id),
  KEY idx_reservations_user_status (user_id, status),
  KEY idx_reservations_status_expires (status, expires_at),
  CONSTRAINT fk_reservations_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
