-- O-7: API Key 生命周期。新增 expires_at / last_used_at；
-- 旧数据 expires_at 留 NULL 视为「永不过期」，与现行为兼容；轮换流程在 M2 与 ops 完成。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE api_keys
  ADD COLUMN expires_at TIMESTAMP(3) NULL DEFAULT NULL COMMENT 'NULL 表示永不过期',
  ADD COLUMN last_used_at TIMESTAMP(3) NULL DEFAULT NULL;

CREATE INDEX idx_api_keys_status_expires ON api_keys (status, expires_at);
