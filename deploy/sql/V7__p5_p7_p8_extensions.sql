-- 套餐/工单消息/发票/退款/审计/公告；价目种子
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO pricing_plans (code, name, price, cycle, status)
VALUES ('metered-default', '按量计费(占位)', 0, 'METERED', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status);

CREATE TABLE IF NOT EXISTS support_ticket_messages (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL COMMENT 'USER|AGENT',
  body TEXT NOT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_msg_ticket (ticket_id),
  CONSTRAINT fk_stm_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets (id),
  CONSTRAINT fk_stm_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoices (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_ref VARCHAR(64) NOT NULL,
  amount BIGINT NOT NULL,
  currency VARCHAR(16) NOT NULL DEFAULT 'CNY',
  pdf_number VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ISSUED',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_pdf_number (pdf_number),
  KEY idx_inv_user (user_id),
  CONSTRAINT fk_inv_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS refund_requests (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  amount BIGINT NOT NULL,
  reason VARCHAR(512) DEFAULT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_refund_user (user_id),
  CONSTRAINT fk_refund_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_events (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  actor VARCHAR(128) NOT NULL,
  action VARCHAR(64) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id VARCHAR(128) DEFAULT NULL,
  detail_json TEXT,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS announcements (
  id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  body TEXT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  starts_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  ends_at TIMESTAMP(3) DEFAULT NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
