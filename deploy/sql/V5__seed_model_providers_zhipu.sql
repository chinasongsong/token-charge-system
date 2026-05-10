-- Seed Zhipu (智谱) provider + glm-4-flash token price placeholders
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO model_providers (code, title, base_url, capability_json, enabled)
VALUES ('zhipu', 'Zhipu GLM', 'https://open.bigmodel.cn/api/paas/v4', NULL, 1)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  base_url = VALUES(base_url),
  enabled = VALUES(enabled);

INSERT INTO model_prices (provider_id, model, pricing_unit, input_micro, output_micro, effective_from)
SELECT id,
  'glm-4-flash',
  'TOKEN',
  120,
  240,
  CURRENT_TIMESTAMP(3)
FROM model_providers
WHERE code = 'zhipu'
ON DUPLICATE KEY UPDATE
  input_micro = VALUES(input_micro),
  output_micro = VALUES(output_micro),
  effective_from = VALUES(effective_from);
