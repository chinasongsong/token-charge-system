-- O-10 客户端幂等键设计：新增幂等键来源与过期时间字段
-- 作者：待填写
-- 日期：2026-05-17

-- 新增 idempotency_source 字段：记录幂等键来源（CLIENT | TRACE_ID_FALLBACK）
ALTER TABLE request_orders 
  ADD COLUMN idempotency_source VARCHAR(20) DEFAULT 'TRACE_ID_FALLBACK' 
    COMMENT '幂等键来源: CLIENT(客户端提供) | TRACE_ID_FALLBACK(回退traceId)' 
    AFTER idempotency_key;

-- 新增 idempotency_expires_at 字段：客户端幂等键过期时间（24小时后清理）
ALTER TABLE request_orders 
  ADD COLUMN idempotency_expires_at DATETIME DEFAULT NULL 
    COMMENT '幂等键过期时间(仅CLIENT来源设置，用于TTL清理)' 
    AFTER idempotency_source;

-- 新增索引：用于 TTL 清理调度器查询过期幂等键
ALTER TABLE request_orders 
  ADD INDEX idx_request_orders_idem_expires (idempotency_expires_at);

-- 更新现有数据：将历史数据的 idempotency_source 设为 TRACE_ID_FALLBACK
-- 注：默认值已设置，无需额外 UPDATE