package com.tokenhub.adapter.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenhub.adapter.infrastructure.billing.BillingSettlementClient;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * O-10 扩展：按网关下发的 {@link BillingSettlementClient#HEADER_IDEMPOTENCY_COMPOSITE} 缓存 Chat 成功响应，
 * 实现用户侧不重复扣费、平台侧不重复调上游模型。
 */
@Component
@EnableConfigurationProperties(ChatIdempotencyCacheProperties.class)
public class RedisChatIdempotencyResponseCache {

  private static final Logger log = LoggerFactory.getLogger(RedisChatIdempotencyResponseCache.class);

  private static final String RESPONSE_KEY_PREFIX = "adapter:chat:idem:resp:";
  private static final String LOCK_KEY_PREFIX = "adapter:chat:idem:lock:";

  private final ObjectProvider<StringRedisTemplate> redisProvider;
  private final ChatIdempotencyCacheProperties properties;
  private final ObjectMapper objectMapper;

  public RedisChatIdempotencyResponseCache(
      ObjectProvider<StringRedisTemplate> redisProvider,
      ChatIdempotencyCacheProperties properties,
      ObjectMapper objectMapper
  ) {
    this.redisProvider = redisProvider;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public boolean isActive() {
    return properties.isEnabled() && redisProvider.getIfAvailable() != null;
  }

  public Optional<JsonNode> get(String compositeKey) {
    StringRedisTemplate redis = redis();
    if (redis == null || compositeKey == null || compositeKey.isBlank()) {
      return Optional.empty();
    }
    try {
      String json = redis.opsForValue().get(responseKey(compositeKey.trim()));
      if (json == null || json.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readTree(json));
    } catch (JsonProcessingException ex) {
      log.warn("idempotency cache deserialize failed, key={}: {}", maskKey(compositeKey), ex.toString());
      return Optional.empty();
    }
  }

  public void put(String compositeKey, JsonNode response) {
    StringRedisTemplate redis = redis();
    if (redis == null || compositeKey == null || compositeKey.isBlank() || response == null) {
      return;
    }
    try {
      String json = objectMapper.writeValueAsString(response);
      long ttl = Math.max(60L, properties.getTtlSeconds());
      redis.opsForValue().set(responseKey(compositeKey.trim()), json, Duration.ofSeconds(ttl));
    } catch (JsonProcessingException ex) {
      log.warn("idempotency cache serialize failed, key={}: {}", maskKey(compositeKey), ex.toString());
    }
  }

  public boolean tryLock(String compositeKey) {
    StringRedisTemplate redis = redis();
    if (redis == null || compositeKey == null || compositeKey.isBlank()) {
      return true;
    }
    String token = UUID.randomUUID().toString();
    long ttl = Math.max(10L, properties.getLockTtlSeconds());
    Boolean ok = redis.opsForValue().setIfAbsent(
        lockKey(compositeKey.trim()),
        token,
        Duration.ofSeconds(ttl)
    );
    return Boolean.TRUE.equals(ok);
  }

  public void releaseLock(String compositeKey) {
    StringRedisTemplate redis = redis();
    if (redis == null || compositeKey == null || compositeKey.isBlank()) {
      return;
    }
    try {
      redis.delete(lockKey(compositeKey.trim()));
    } catch (Exception ex) {
      log.debug("idempotency lock release skipped: {}", ex.toString());
    }
  }

  /**
   * 另一并发请求持有锁时轮询缓存，直至命中或超时。
   */
  public Optional<JsonNode> waitForValue(String compositeKey) {
    if (!isActive() || compositeKey == null || compositeKey.isBlank()) {
      return Optional.empty();
    }
    long deadline = System.currentTimeMillis() + Math.max(1000L, properties.getWaitMs());
    while (System.currentTimeMillis() < deadline) {
      Optional<JsonNode> hit = get(compositeKey);
      if (hit.isPresent()) {
        return hit;
      }
      try {
        Thread.sleep(100L);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    return Optional.empty();
  }

  public static boolean isCacheableResponse(JsonNode response) {
    if (response == null || !response.isObject()) {
      return false;
    }
    if (response.has("choices") && response.get("choices").isArray() && !response.get("choices").isEmpty()) {
      return true;
    }
    if (response.has("usage") && response.get("usage").isObject()) {
      JsonNode u = response.get("usage");
      return u.path("prompt_tokens").asLong(0) > 0 || u.path("completion_tokens").asLong(0) > 0;
    }
    return false;
  }

  private StringRedisTemplate redis() {
    if (!properties.isEnabled()) {
      return null;
    }
    return redisProvider.getIfAvailable();
  }

  private static String responseKey(String compositeKey) {
    return RESPONSE_KEY_PREFIX + compositeKey;
  }

  private static String lockKey(String compositeKey) {
    return LOCK_KEY_PREFIX + compositeKey;
  }

  private static String maskKey(String key) {
    if (key == null || key.length() < 12) {
      return "...";
    }
    return "..." + key.substring(key.length() - 12);
  }
}
