-- Idempotency for payment -> billing balance credit (cross-service source ref, e.g. payment order_no).
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS balance_topup_receipts (
  source_ref VARCHAR(128) NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  amount BIGINT NOT NULL COMMENT 'same unit as account_balance.balance',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_topup_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
