package com.tokenhub.gateway.infrastructure.cache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * O-5 网关侧 API Key 解析缓存。
 *
 * <ul>
 *   <li>命中正向：返回 {@code Entry.present}，跳过 billing HTTP；
 *   <li>命中负向（"NEG"）：短 TTL 抑制无效 Key 击穿；
 *   <li>未命中：返回 {@code Mono.empty()}，调用方走 HTTP 后回写。
 * </ul>
 *
 * <p>失效：依赖 TTL 上界；Key 禁用后最迟在 TTL 内生效（默认 60s）。如需即时失效，
 * 后续可扩展 billing 在 disable 时通过 Redis PUB 主动驱逐（M2）。
 */
@Component
public class ApiKeyResolutionCache {

  private static final Logger log = LoggerFactory.getLogger(ApiKeyResolutionCache.class);
  private static final String NEGATIVE = "NEG";

  private final ReactiveStringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  @Value("${tokenhub.gateway.apikey-cache.enabled:false}")
  private boolean enabled;

  @Value("${tokenhub.gateway.apikey-cache.positive-ttl-seconds:60}")
  private long positiveTtlSeconds;

  @Value("${tokenhub.gateway.apikey-cache.negative-ttl-seconds:10}")
  private long negativeTtlSeconds;

  public ApiKeyResolutionCache(ReactiveStringRedisTemplate redis, ObjectMapper objectMapper) {
    this.redis = redis;
    this.objectMapper = objectMapper;
  }

  public boolean isEnabled() {
    return enabled;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Entry(long userId, long apiKeyId, boolean negative) {
    public static Entry present(long userId, long apiKeyId) {
      return new Entry(userId, apiKeyId, false);
    }

    public static Entry notFound() {
      return new Entry(-1L, -1L, true);
    }
  }

  private static String keyOf(String fingerprint) {
    return "cache:gw:apikey:fp:" + fingerprint;
  }

  public Mono<Entry> get(String fingerprint) {
    if (!enabled || fingerprint == null || fingerprint.isBlank()) {
      return Mono.empty();
    }
    return redis.opsForValue()
        .get(keyOf(fingerprint))
        .flatMap(raw -> {
          if (raw == null || raw.isEmpty()) {
            return Mono.empty();
          }
          if (NEGATIVE.equals(raw)) {
            return Mono.just(Entry.notFound());
          }
          try {
            return Mono.just(objectMapper.readValue(raw, Entry.class));
          } catch (JsonProcessingException ex) {
            log.warn("ApiKeyResolutionCache decode error: {}", ex.getMessage());
            return Mono.empty();
          }
        })
        .onErrorResume(ex -> {
          log.warn("ApiKeyResolutionCache get error: {}", ex.getMessage());
          return Mono.empty();
        });
  }

  public Mono<Void> putPresent(String fingerprint, long userId, long apiKeyId) {
    if (!enabled) {
      return Mono.empty();
    }
    try {
      String json = objectMapper.writeValueAsString(Entry.present(userId, apiKeyId));
      return redis.opsForValue()
          .set(keyOf(fingerprint), json, Duration.ofSeconds(Math.max(1, positiveTtlSeconds)))
          .then()
          .onErrorResume(ex -> {
            log.warn("ApiKeyResolutionCache put error: {}", ex.getMessage());
            return Mono.empty();
          });
    } catch (JsonProcessingException ex) {
      return Mono.empty();
    }
  }

  public Mono<Void> putNegative(String fingerprint) {
    if (!enabled || negativeTtlSeconds <= 0) {
      return Mono.empty();
    }
    return redis.opsForValue()
        .set(keyOf(fingerprint), NEGATIVE, Duration.ofSeconds(negativeTtlSeconds))
        .then()
        .onErrorResume(ex -> {
          log.warn("ApiKeyResolutionCache putNegative error: {}", ex.getMessage());
          return Mono.empty();
        });
  }
}
