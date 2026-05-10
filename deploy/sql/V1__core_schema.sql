-- Core relational schema bootstrap (Flyway-compatible naming — apply manually until Flyway wired per service).

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(255) DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_devices (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  fingerprint VARCHAR(191) DEFAULT NULL,
  user_agent VARCHAR(512) DEFAULT NULL,
  ip_address VARCHAR(64) DEFAULT NULL,
  last_login_at TIMESTAMP(3) DEFAULT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_devices_user_id (user_id),
  CONSTRAINT fk_devices_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS api_keys (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(191) DEFAULT NULL,
  fingerprint CHAR(64) NOT NULL COMMENT 'SHA-256hex of plaintext key',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_keys_user_id (user_id),
  UNIQUE KEY uk_keys_fingerprint (fingerprint),
  CONSTRAINT fk_keys_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS account_balance (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  balance BIGINT NOT NULL COMMENT 'fractional milli-tokens arbitrary unit',
  version BIGINT NOT NULL DEFAULT 0,
  currency VARCHAR(16) NOT NULL DEFAULT 'TOKEN',
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_balance_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS request_orders (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  trace_id VARCHAR(191) DEFAULT NULL,
  api_key_id BIGINT DEFAULT NULL,
  user_id BIGINT NOT NULL,
  provider_code VARCHAR(64) NOT NULL,
  model_name VARCHAR(191) NOT NULL,
  billing_status VARCHAR(32) NOT NULL DEFAULT 'AUTHORIZED',
  input_tokens BIGINT NOT NULL DEFAULT 0,
  output_tokens BIGINT NOT NULL DEFAULT 0,
  amount BIGINT NOT NULL COMMENT 'pricing unit debited after completion',
  idempotency_key VARCHAR(191) DEFAULT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_request_orders_idem (idempotency_key),
  KEY idx_request_orders_trace (trace_id),
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usage_ledger (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  request_order_id BIGINT NOT NULL,
  entry_type VARCHAR(32) NOT NULL,
  quantity BIGINT NOT NULL COMMENT 'logical units billed',
  idempotency_key VARCHAR(191) DEFAULT NULL,
  detail_json TEXT,
  recorded_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_usage_user (user_id),
  UNIQUE KEY uk_usage_idempotency (idempotency_key),
  CONSTRAINT fk_usage_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_usage_order FOREIGN KEY (request_order_id) REFERENCES request_orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_orders (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  channel VARCHAR(32) NOT NULL COMMENT 'mock|wechat|alipay',
  status VARCHAR(32) NOT NULL DEFAULT 'INIT',
  amount BIGINT NOT NULL COMMENT 'minor currency unit',
  currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
  metadata_json TEXT,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_pay_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pricing_plans (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(191) NOT NULL,
  price BIGINT NOT NULL COMMENT 'minor currency snapshot',
  cycle VARCHAR(32) NOT NULL COMMENT 'MONTHLY|yearly|METERED',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_subscriptions (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  started_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  ends_at TIMESTAMP(3) DEFAULT NULL,
  KEY idx_subscription_user (user_id),
  KEY idx_subscription_plan (plan_id),
  CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES pricing_plans (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS model_providers (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE COMMENT 'dashscope|zhipu',
  title VARCHAR(191) NOT NULL,
  base_url VARCHAR(512) DEFAULT NULL,
  capability_json TEXT COMMENT 'capabilities',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS model_prices (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  provider_id BIGINT NOT NULL,
  model VARCHAR(191) NOT NULL,
  pricing_unit VARCHAR(64) NOT NULL COMMENT 'TOKEN|INVOCATION',
  input_micro BIGINT DEFAULT NULL COMMENT 'micro price per thousand input tokens',
  output_micro BIGINT DEFAULT NULL COMMENT 'micro price per thousand output tokens',
  effective_from TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_model_price (provider_id, model, pricing_unit),
  CONSTRAINT fk_price_provider FOREIGN KEY (provider_id) REFERENCES model_providers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS risk_events (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT DEFAULT NULL,
  event_type VARCHAR(64) NOT NULL,
  severity VARCHAR(16) NOT NULL DEFAULT 'INFO',
  context_json TEXT,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_risk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_tickets (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
  last_message_preview VARCHAR(512) DEFAULT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_ticket_user (user_id),
  CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
