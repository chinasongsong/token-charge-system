-- Seed default DeepSeek provider row (base URL can be overridden per environment).
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO model_providers (code, title, base_url, capability_json, enabled)
VALUES ('deepseek', 'DeepSeek', 'https://api.deepseek.com', NULL, 1)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  base_url = VALUES(base_url),
  enabled = VALUES(enabled);
