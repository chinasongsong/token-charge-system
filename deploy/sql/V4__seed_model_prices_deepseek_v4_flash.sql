-- Token-unit micro prices per 1000 tokens (placeholders for local dev)
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO model_prices (provider_id, model, pricing_unit, input_micro, output_micro, effective_from)
SELECT id,
  'deepseek-v4-flash',
  'TOKEN',
  100,
  200,
  CURRENT_TIMESTAMP(3)
FROM model_providers
WHERE code = 'deepseek'
ON DUPLICATE KEY UPDATE
  input_micro = VALUES(input_micro),
  output_micro = VALUES(output_micro),
  effective_from = VALUES(effective_from);
