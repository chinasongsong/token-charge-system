package com.tokenhub.adapter.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tokenhub.adapter.idempotency-cache")
public class ChatIdempotencyCacheProperties {

  /** 是否启用 Chat 响应幂等缓存（需 Redis）。 */
  private boolean enabled = true;

  /** 成功响应缓存 TTL（秒），默认 24h，与 billing CLIENT 幂等窗口对齐。 */
  private long ttlSeconds = 86400L;

  /** 同键并发时 in-flight 锁 TTL（秒）。 */
  private long lockTtlSeconds = 120L;

  /** 等待另一请求写入缓存的最长时间（毫秒）。 */
  private long waitMs = 30_000L;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getTtlSeconds() {
    return ttlSeconds;
  }

  public void setTtlSeconds(long ttlSeconds) {
    this.ttlSeconds = ttlSeconds;
  }

  public long getLockTtlSeconds() {
    return lockTtlSeconds;
  }

  public void setLockTtlSeconds(long lockTtlSeconds) {
    this.lockTtlSeconds = lockTtlSeconds;
  }

  public long getWaitMs() {
    return waitMs;
  }

  public void setWaitMs(long waitMs) {
    this.waitMs = waitMs;
  }
}
